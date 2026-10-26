-- Automatic closure of unconfirmed solutions (docs/api/ui-extensions.md).

-- The system, not a user, records the automatic closure: its history entry has no actor (NULL). The history
-- read models show it as 'Sistema'.
ALTER TABLE request_history MODIFY actor_id BIGINT NULL;

-- The closure job looks for the requests in RESUELTA; the existing indexes start with area or technician.
CREATE INDEX ix_service_requests_status ON service_requests (status);
