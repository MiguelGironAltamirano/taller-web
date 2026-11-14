-- Allows system-initiated history entries (e.g. auto-close after 72 h) where there is no human actor.
ALTER TABLE request_history
  MODIFY actor_id BIGINT NULL;
