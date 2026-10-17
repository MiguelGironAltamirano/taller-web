INSERT INTO areas (id, name) VALUES
  (1, 'Mantenimiento'),
  (2, 'Infraestructura'),
  (3, 'Soporte Informático'),
  (4, 'Servicios Administrativos');

INSERT INTO categories (id, name, default_area_id) VALUES
  (1, 'Electricidad', 1),
  (2, 'Gasfitería', 1),
  (3, 'Mobiliario', 2),
  (4, 'Aulas y laboratorios', 2),
  (5, 'Equipos de cómputo', 3),
  (6, 'Red e Internet', 3),
  (7, 'Trámites administrativos', 4);

INSERT INTO priorities (id, name, sla_hours, sort_order) VALUES
  (1, 'Crítica', 8, 1),
  (2, 'Alta', 24, 2),
  (3, 'Media', 72, 3),
  (4, 'Baja', 120, 4);

INSERT INTO locations (id, name, kind, parent_id) VALUES
  (1, 'Facultad de Ingeniería de Sistemas e Informática', 'FACULTAD', NULL),
  (2, 'Pabellón Principal', 'PABELLON', 1),
  (3, 'Aula 101', 'AMBIENTE', 2),
  (4, 'Laboratorio 2', 'AMBIENTE', 2),
  (5, 'Facultad de Ciencias Matemáticas', 'FACULTAD', NULL),
  (6, 'Pabellón A', 'PABELLON', 5),
  (7, 'Aula 201', 'AMBIENTE', 6),
  (8, 'Biblioteca', 'AMBIENTE', 6);
