-- Fase 7: permite asignar automáticamente IDs de nuevos platillos.
-- Ejecutar UNA VEZ, desde phpMyAdmin, sobre la base exclusiva de Restaurante.
-- Conserva IDs y filas existentes, incluyendo referencias en pedido_detalles.
-- No modifica otros esquemas ni elimina datos.
USE restaurante_portafolio;

ALTER TABLE platillos
    MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;
