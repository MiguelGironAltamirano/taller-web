-- Load-test data: @n extra requests on top of db/seed-demo, so the list, the filters and the text search run against
-- a realistic volume instead of the ~70 demo requests. ONLY for the load-test stack (infra/k6/run.sh), never for
-- staging or production. Codes use the year 2025 (SOL-2025-000001...), so they never collide with the codes the
-- application generates for new requests. Values are deterministic (derived from the row number), so two runs on a
-- fresh stack produce the same data and comparable results.

SET SESSION cte_max_recursion_depth = 1000000;
SET @n = 20000;

DROP TABLE IF EXISTS k6_requester, k6_category, k6_room, k6_priority, k6_technician, k6_technician_count;

CREATE TABLE k6_requester AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM users WHERE role = 'SOLICITANTE' AND active;
CREATE TABLE k6_category AS
  SELECT id, default_area_id AS area_id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM categories WHERE active;
CREATE TABLE k6_room AS
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM locations WHERE kind = 'AMBIENTE';
CREATE TABLE k6_priority AS
  SELECT id, sla_hours, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM priorities;
CREATE TABLE k6_technician AS
  SELECT id, area_id, ROW_NUMBER() OVER (PARTITION BY area_id ORDER BY id) - 1 AS rn
  FROM users WHERE role = 'TECNICO' AND active AND area_id IS NOT NULL;
CREATE TABLE k6_technician_count AS SELECT area_id, COUNT(*) AS total FROM k6_technician GROUP BY area_id;

SELECT COUNT(*) INTO @requesters FROM k6_requester;
SELECT COUNT(*) INTO @categories FROM k6_category;
SELECT COUNT(*) INTO @rooms FROM k6_room;
SELECT COUNT(*) INTO @priorities FROM k6_priority;

-- Status mix (per 20 requests): 3 registered, 2 in evaluation, 3 assigned, 2 in attention, 2 resolved,
-- 7 closed and 1 cancelled; roughly what a semester of use looks like, with most requests already closed.
INSERT INTO service_requests (code, requester_id, category_id, location_id, description, priority_id,
    requested_priority_id, status, area_id, technician_id, closure_reason, solution, created_at, due_at,
    requested_due_at, updated_at, version)
WITH RECURSIVE seq (i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM seq WHERE i < @n),
rows_ AS (
  SELECT i,
         (SELECT id FROM k6_requester WHERE rn = (i * 7) % @requesters) AS requester_id,
         (SELECT id FROM k6_category WHERE rn = (i * 13) % @categories) AS category_id,
         (SELECT area_id FROM k6_category WHERE rn = (i * 13) % @categories) AS area_id,
         (SELECT id FROM k6_room WHERE rn = (i * 31) % @rooms) AS location_id,
         (SELECT id FROM k6_priority WHERE rn = (i * 3) % @priorities) AS priority_id,
         (SELECT sla_hours FROM k6_priority WHERE rn = (i * 3) % @priorities) AS sla_hours,
         ELT((i % 20) + 1,
             'REGISTRADA', 'REGISTRADA', 'REGISTRADA', 'EN_EVALUACION', 'EN_EVALUACION',
             'ASIGNADA', 'ASIGNADA', 'ASIGNADA', 'EN_ATENCION', 'EN_ATENCION', 'RESUELTA', 'RESUELTA',
             'CERRADA', 'CERRADA', 'CERRADA', 'CERRADA', 'CERRADA', 'CERRADA', 'CERRADA', 'CANCELADA') AS status,
         UTC_TIMESTAMP(6) - INTERVAL (i % 180) DAY - INTERVAL (i % 1440) MINUTE AS created_at
  FROM seq
)
SELECT CONCAT('SOL-2025-', LPAD(r.i, 6, '0')),
       r.requester_id, r.category_id, r.location_id,
       CONCAT(ELT((r.i % 10) + 1,
                  'El foco del aula parpadea y deja el ambiente a oscuras',
                  'El proyector no enciende y la clase empieza pronto',
                  'Gotea el caño del lavadero y se moja el piso',
                  'La carpeta tiene el tablero roto y no se puede usar',
                  'No hay internet en el laboratorio desde la mañana',
                  'La computadora del docente no arranca',
                  'El tomacorriente hace chispas al conectar',
                  'La puerta no cierra bien y la cerradura está trabada',
                  'Solicito constancia de matrícula para un trámite',
                  'El aire acondicionado hace ruido y no enfría'),
              ' (carga ', r.i, ').'),
       r.priority_id, r.priority_id, r.status, r.area_id,
       CASE WHEN r.status IN ('ASIGNADA', 'EN_ATENCION', 'RESUELTA', 'CERRADA')
            THEN (SELECT t.id FROM k6_technician t JOIN k6_technician_count c ON c.area_id = t.area_id
                  WHERE t.area_id = r.area_id AND t.rn = r.i % c.total) END,
       CASE WHEN r.status = 'CERRADA' THEN 'CONFIRMADA' END,
       CASE WHEN r.status IN ('RESUELTA', 'CERRADA') THEN 'Se revisó y se corrigió la falla reportada.' END,
       r.created_at,
       r.created_at + INTERVAL r.sla_hours HOUR,
       r.created_at + INTERVAL r.sla_hours HOUR,
       r.created_at,
       0
FROM rows_ r;

-- The creation entry of each one, as the application writes it (the list does not read the history, the detail
-- and the dashboard do).
INSERT INTO request_history (request_id, from_status, to_status, actor_id, technician_id, reason, created_at, internal)
SELECT id, NULL, 'REGISTRADA', requester_id, NULL, 'Solicitud creada', created_at, FALSE
FROM service_requests WHERE code LIKE 'SOL-2025-%';

DROP TABLE k6_requester, k6_category, k6_room, k6_priority, k6_technician, k6_technician_count;

ANALYZE TABLE service_requests, request_history;

SELECT COUNT(*) AS load_requests FROM service_requests WHERE code LIKE 'SOL-2025-%';
