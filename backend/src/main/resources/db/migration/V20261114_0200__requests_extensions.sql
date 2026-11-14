-- Extensions of the requests module (docs/api/ui-extensions.md): proposed solution and history notes.
-- The AUTOMATICA closure reason (V20261113_1020) and the ratings table (V20261113_1000) come from earlier migrations.

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

-- Changes that keep the status (e.g. 'Prioridad Alta → Prioridad Media').
ALTER TABLE request_history ADD COLUMN note VARCHAR(200) NULL AFTER reason;

-- The satisfaction read port lists the ratings given in a period (RatingQueries.findCreatedBetween).
CREATE INDEX ix_ratings_created ON ratings (created_at);
