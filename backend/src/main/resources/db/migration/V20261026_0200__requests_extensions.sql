-- Extensions of the requests module (docs/api/ui-extensions.md): proposed solution, history notes, automatic
-- closure and satisfaction ratings.

-- Solution proposed by the technician when resolving (at most StateMachine.MAX_REASON_LENGTH characters).
ALTER TABLE service_requests ADD COLUMN solution VARCHAR(400) NULL AFTER closure_reason;

-- Requests resolved before this column existed: take the text of their latest resolution from the history.
UPDATE service_requests r
   SET r.solution = (
       SELECT SUBSTRING(h.reason, CHAR_LENGTH('Solución: ') + 1)
         FROM request_history h
        WHERE h.request_id = r.id
          AND h.to_status = 'RESUELTA'
          AND h.reason LIKE 'Solución: %'
        ORDER BY h.created_at DESC, h.id DESC
        LIMIT 1)
 WHERE r.status IN ('RESUELTA', 'CERRADA');

-- Closure by the system once the confirmation period expires.
ALTER TABLE service_requests DROP CHECK ck_service_requests_closure;
ALTER TABLE service_requests ADD CONSTRAINT ck_service_requests_closure
  CHECK (closure_reason IS NULL OR closure_reason IN ('NO_PROCEDE', 'DUPLICADA', 'CONFIRMADA', 'AUTOMATICA'));

-- Changes that keep the status (e.g. 'Prioridad Alta → Prioridad Media').
ALTER TABLE request_history ADD COLUMN note VARCHAR(200) NULL AFTER reason;

-- One optional satisfaction rating per request, given by its requester after confirming the solution.
CREATE TABLE request_ratings (
  request_id BIGINT PRIMARY KEY,
  requester_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  comment VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_request_ratings_request FOREIGN KEY (request_id) REFERENCES service_requests (id) ON DELETE CASCADE,
  CONSTRAINT fk_request_ratings_requester FOREIGN KEY (requester_id) REFERENCES users (id),
  CONSTRAINT ck_request_ratings_score CHECK (score BETWEEN 1 AND 5)
);

CREATE INDEX ix_request_ratings_created ON request_ratings (created_at);
