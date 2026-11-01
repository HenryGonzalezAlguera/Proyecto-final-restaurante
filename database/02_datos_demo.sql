-- Datos ficticios para el catálogo de Restaurante (solo desarrollo local).
-- Repetible: si existe el ID, no sobrescribe cambios del usuario.
-- Ejecutar DESPUÉS de database/01_esquema.sql.
USE restaurante_portafolio;

INSERT INTO platillos (id, nombre, costo_ingredientes, precio, activo) VALUES
  (1, 'Arroz con Pollo', 3000.00, 4000.00, TRUE),
  (2, 'La Carnita Roja', 4000.00, 6000.00, TRUE),
  (3, 'Casado Típico', 2200.00, 3800.00, TRUE),
  (4, 'Pasta Alfredo', 2500.00, 4800.00, TRUE),
  (5, 'Ensalada de la Casa', 1450.00, 3000.00, TRUE)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO empleados (id, nombre, puesto) VALUES
  (1, 'Ana Rodríguez', 'Cocinera'),
  (2, 'Pablo Ortiz', 'Cocinero'),
  (3, 'Luis Calderón', 'Cajero')
ON DUPLICATE KEY UPDATE id = id;
