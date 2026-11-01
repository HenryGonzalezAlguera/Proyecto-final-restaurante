-- Restaurante — esquema independiente para MySQL 8
-- No modifica ninguna otra base de datos de XAMPP.
CREATE DATABASE IF NOT EXISTS restaurante_portafolio
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE restaurante_portafolio;

CREATE TABLE IF NOT EXISTS platillos (
    id INT NOT NULL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    costo_ingredientes DECIMAL(12, 2) NOT NULL,
    precio DECIMAL(12, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_platillo_id CHECK (id > 0),
    CONSTRAINT chk_platillo_costo CHECK (costo_ingredientes >= 0),
    CONSTRAINT chk_platillo_precio CHECK (precio > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS empleados (
    id INT NOT NULL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    puesto VARCHAR(100) NOT NULL,
    CONSTRAINT chk_empleado_id CHECK (id > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pedidos (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_pedido_cliente CHECK (cliente_id > 0),
    CONSTRAINT chk_pedido_estado CHECK (
        estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'CANCELADO')
    ),
    INDEX idx_pedidos_estado_id (estado, id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pedido_detalles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    platillo_id INT NOT NULL,
    nombre_platillo VARCHAR(150) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_precio CHECK (precio_unitario > 0),
    CONSTRAINT fk_detalles_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedidos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_detalles_platillo FOREIGN KEY (platillo_id)
        REFERENCES platillos(id) ON DELETE RESTRICT,
    INDEX idx_detalles_pedido_id (pedido_id, id)
) ENGINE = InnoDB;
