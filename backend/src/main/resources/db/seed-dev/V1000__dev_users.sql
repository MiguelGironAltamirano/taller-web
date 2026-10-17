INSERT INTO users (id, full_name, email, password_hash, role, requester_type, area_id, created_at) VALUES
  (1, 'Administrador Demo', 'admin@unmsm.edu.pe', '$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy', 'ADMINISTRADOR', NULL, NULL, UTC_TIMESTAMP(6)),
  (2, 'Supervisora Mantenimiento', 'supervisor.mantenimiento@unmsm.edu.pe', '$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy', 'SUPERVISOR', NULL, 1, UTC_TIMESTAMP(6)),
  (3, 'Técnico Mantenimiento', 'tecnico.mantenimiento@unmsm.edu.pe', '$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy', 'TECNICO', NULL, 1, UTC_TIMESTAMP(6)),
  (4, 'Estudiante Demo', 'estudiante@unmsm.edu.pe', '$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy', 'SOLICITANTE', 'ESTUDIANTE', NULL, UTC_TIMESTAMP(6));
