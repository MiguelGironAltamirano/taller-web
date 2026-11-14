-- Automatic closure of unconfirmed solutions (docs/api/ui-extensions.md). The history entry the system writes has no
-- actor (request_history.actor_id is nullable since V20261113_1010); the read models show it as 'Sistema'.

-- The closure job looks for the requests in RESUELTA; the existing indexes start with area or technician.
CREATE INDEX ix_service_requests_status ON service_requests (status);
