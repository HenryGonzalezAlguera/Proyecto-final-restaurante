-- Fase 8: usuarios locales de Restaurante con contraseñas BCrypt.
-- Aplicar una sola vez en la base dedicada; no afecta pedidos ni otras bases.
-- NUNCA guardar aquí contraseñas reales ni usuarios de MySQL.
USE restaurante_portafolio;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuarios_roles CHECK (rol IN ('ADMIN', 'EMPLEADO')),
    CONSTRAINT chk_usuarios_nombre CHECK (CHAR_LENGTH(usuario) BETWEEN 3 AND 64)
) ENGINE=InnoDB;
