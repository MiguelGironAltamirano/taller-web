-- GET /requests orders by registration date by default (newest first) and pages with LIMIT/OFFSET.
-- An administrator lists every request, so without an index on created_at MySQL sorts the whole table
-- for each page; InnoDB appends the primary key to secondary indexes, which covers the id tie-breaker.
CREATE INDEX ix_service_requests_created ON service_requests (created_at);

-- A requester (the most common role) lists their own requests newest first: equality on requester_id
-- followed by the order column lets the page be read from the index without a filesort.
-- Supervisors and technicians keep their existing (area_id, status) and (technician_id, status) indexes.
CREATE INDEX ix_service_requests_requester_created ON service_requests (requester_id, created_at);
