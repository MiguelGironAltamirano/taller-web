-- More demo users (local and staging only, like V1000) so every area can run the whole flow and the edge cases can
-- be tried by hand. Password of all of them: Demo12345!
--
-- * Infraestructura has two active technicians (reassignment) and a deactivated one (cannot log in, never offered).
-- * Mantenimiento keeps only the users of V1000: the workflow tests expect exactly its technicians.
-- * Requesters of the three types, a second student (one requester never sees another's requests) and a deactivated
--   one.
--
-- Ids are left to AUTO_INCREMENT, and a user whose email already exists (for example created by hand from the
-- administration screen) is skipped instead of failing the migration.
INSERT INTO users (full_name, email, password_hash, role, requester_type, area_id, active, created_at)
SELECT v.full_name, v.email, '$2b$10$hOCLn2iSSfARxF.PH/fQoeUJGY3u2dOY7aug6hnxrdiqia2rKKXvy', v.role,
       v.requester_type, v.area_id, v.active, UTC_TIMESTAMP(6)
  FROM (VALUES
    ROW('Supervisor Infraestructura', 'supervisor.infraestructura@unmsm.edu.pe', 'SUPERVISOR', NULL, 2, TRUE),
    ROW('Técnico Infraestructura', 'tecnico.infraestructura@unmsm.edu.pe', 'TECNICO', NULL, 2, TRUE),
    ROW('Técnica Infraestructura', 'tecnica.infraestructura@unmsm.edu.pe', 'TECNICO', NULL, 2, TRUE),
    ROW('Técnico Cesado', 'tecnico.cesado@unmsm.edu.pe', 'TECNICO', NULL, 2, FALSE),
    ROW('Supervisora Soporte Informático', 'supervisora.informatica@unmsm.edu.pe', 'SUPERVISOR', NULL, 3, TRUE),
    ROW('Técnico Soporte Informático', 'tecnico.informatica@unmsm.edu.pe', 'TECNICO', NULL, 3, TRUE),
    ROW('Supervisor Servicios Administrativos', 'supervisor.administrativo@unmsm.edu.pe', 'SUPERVISOR', NULL, 4, TRUE),
    ROW('Técnica Servicios Administrativos', 'tecnica.administrativa@unmsm.edu.pe', 'TECNICO', NULL, 4, TRUE),
    ROW('Docente Demo', 'docente@unmsm.edu.pe', 'SOLICITANTE', 'DOCENTE', NULL, TRUE),
    ROW('Administrativo Demo', 'administrativo@unmsm.edu.pe', 'SOLICITANTE', 'ADMINISTRATIVO', NULL, TRUE),
    ROW('Segundo Estudiante Demo', 'estudiante2@unmsm.edu.pe', 'SOLICITANTE', 'ESTUDIANTE', NULL, TRUE),
    ROW('Solicitante Inactivo', 'solicitante.inactivo@unmsm.edu.pe', 'SOLICITANTE', 'ESTUDIANTE', NULL, FALSE)
  ) AS v (full_name, email, role, requester_type, area_id, active)
 WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);
