-- Fase 9C: bitácora de acciones posteriores a cambios exitosos.
-- Crear usando phpMyAdmin en la base dedicada, sin tocar los pedidos históricos.
-- El usuario técnico restaurante_app necesita SELECT e INSERT sobre esta tabla.
USE restaurante_portafolio;

CREATE TABLE IF NOT EXISTS auditoria_operaciones (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actor VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    accion VARCHAR(48) NOT NULL,
    entidad VARCHAR(16) NOT NULL,
    referencia VARCHAR(64) NOT NULL,
    detalle VARCHAR(48) NOT NULL DEFAULT '',
    INDEX idx_auditoria_fecha (fecha, id),
    INDEX idx_auditoria_actor (actor, id)
) ENGINE=InnoDB;
