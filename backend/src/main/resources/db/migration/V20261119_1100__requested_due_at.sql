-- Estimated date the requester sees instead of the due date: registration + the SLA hours of the priority they chose,
-- fixed at registration (docs/api/contrato-v1.md, "Prioridad que ve el solicitante"). Nullable only because the demo
-- seed (R__demo_data.sql) inserts its rows without it and fills it right after; the application always writes it.
ALTER TABLE service_requests ADD COLUMN requested_due_at DATETIME(6) NULL AFTER due_at;

-- Existing requests: their due date when the priority never changed; otherwise rebuilt with the current SLA hours
-- of the chosen priority.
UPDATE service_requests r
  JOIN priorities rp ON rp.id = r.requested_priority_id
   SET r.requested_due_at = CASE WHEN r.requested_priority_id = r.priority_id THEN r.due_at
                                 ELSE r.created_at + INTERVAL rp.sla_hours HOUR END;
