-- The requester keeps seeing the priority they chose; the supervisor's change only affects the staff
-- (docs/api/contrato-v1.md, "Prioridad que ve el solicitante").

-- Priority chosen at registration. Nullable only because the demo seed (R__demo_data.sql) inserts its rows without
-- it and fills it right after; the application always writes it.
ALTER TABLE service_requests ADD COLUMN requested_priority_id BIGINT NULL AFTER priority_id;
ALTER TABLE service_requests ADD CONSTRAINT fk_service_requests_requested_priority
  FOREIGN KEY (requested_priority_id) REFERENCES priorities (id);

-- History entries the requester does not see (priority changes).
ALTER TABLE request_history ADD COLUMN internal BOOLEAN NOT NULL DEFAULT FALSE AFTER note;

-- Existing requests: the priority before their first change (from the history note
-- 'Prioridad Alta → Prioridad Media'), or the current one when it never changed.
UPDATE service_requests r
   SET r.requested_priority_id = COALESCE((
       SELECT p.id
         FROM request_history h
         JOIN priorities p
           ON p.name = SUBSTRING(SUBSTRING_INDEX(h.note, ' → ', 1), CHAR_LENGTH('Prioridad ') + 1)
        WHERE h.request_id = r.id
          AND h.note LIKE 'Prioridad % → Prioridad %'
        ORDER BY h.created_at, h.id
        LIMIT 1), r.priority_id);

UPDATE request_history SET internal = TRUE WHERE note LIKE 'Prioridad % → Prioridad %';
