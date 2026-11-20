-- The requester may cancel a request before a technician is assigned, and restore it within 7 days
-- (docs/api/contrato-v1.md «Cancelar y restaurar una solicitud»). The status it had and the moment of the
-- cancellation are read from request_history, so the request needs no new columns.
ALTER TABLE service_requests DROP CHECK ck_service_requests_status;
ALTER TABLE service_requests ADD CONSTRAINT ck_service_requests_status
  CHECK (status IN ('REGISTRADA', 'EN_EVALUACION', 'ASIGNADA', 'EN_ATENCION', 'RESUELTA', 'CERRADA', 'CANCELADA'));
