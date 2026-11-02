-- Fase 7: migración conservadora del ID de platillos a AUTO_INCREMENT.
-- IMPORTANTE: hacer un respaldo, detener la aplicación y ejecutar UNA VEZ.
-- No borra registros ni modifica los IDs de platillos o detalles existentes.
-- DDL MySQL hace commits implícitos: si falla un paso, revisar la BD antes de continuar.
USE restaurante_portafolio;

-- La clave foránea impide modificar una columna referenciada directamente.
ALTER TABLE pedido_detalles DROP FOREIGN KEY fk_detalles_platillo;

-- MySQL no permite un CHECK explícito sobre una columna AUTO_INCREMENT.
ALTER TABLE platillos DROP CHECK chk_platillo_id;

ALTER TABLE platillos MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

-- Restauramos la relación original para mantener integridad histórica.
ALTER TABLE pedido_detalles ADD CONSTRAINT fk_detalles_platillo
    FOREIGN KEY (platillo_id) REFERENCES platillos(id) ON DELETE RESTRICT;
