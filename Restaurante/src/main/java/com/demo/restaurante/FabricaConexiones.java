package com.demo.restaurante;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstracción mínima para crear conexiones JDBC, sin introducir Spring todavía.
 */
@FunctionalInterface
public interface FabricaConexiones {
    Connection abrir() throws SQLException;
}
