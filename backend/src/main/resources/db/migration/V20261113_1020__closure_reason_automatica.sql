-- Requests the system closes after 72 h without an answer from the requester (contract: ClosureReason AUTOMATICA).
ALTER TABLE service_requests DROP CHECK ck_service_requests_closure;
ALTER TABLE service_requests ADD CONSTRAINT ck_service_requests_closure
  CHECK (closure_reason IS NULL OR closure_reason IN ('NO_PROCEDE', 'DUPLICADA', 'CONFIRMADA', 'AUTOMATICA'));
