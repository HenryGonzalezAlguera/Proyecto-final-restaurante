package com.demo.restaurante.web;

import com.demo.restaurante.DetallePedido;
import com.demo.restaurante.EstadoPedido;
import com.demo.restaurante.Pedido;
import com.demo.restaurante.Platillo;
import com.demo.restaurante.RepositorioRestauranteJdbc;
import com.demo.restaurante.RegistroPlatillo;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Casos de uso web sobre el repositorio JDBC ya probado en las fases anteriores.
 * La política de estados continúa en Pedido; no se duplica aquí.
 */
@Service
public class ServicioRestauranteWeb {
    private final RepositorioRestauranteJdbc repositorio;

    public ServicioRestauranteWeb(RepositorioRestauranteJdbc repositorio) {
        this.repositorio = repositorio;
    }

    public List<Platillo> listarPlatillos() throws SQLException {
        return repositorio.listarPlatillos();
    }

    public List<RegistroPlatillo> listarCatalogoAdministracion() throws SQLException {
        return repositorio.listarCatalogoAdministracion();
    }

    public Platillo crearPlatillo(String nombre, BigDecimal costo, BigDecimal precio)
            throws SQLException {
        validarFormulario(nombre, costo, precio);
        return repositorio.crearPlatillo(nombre, costo, precio);
    }

    public RegistroPlatillo actualizarPlatillo(int id, String nombre, BigDecimal costo,
                                               BigDecimal precio) throws SQLException {
        validarFormulario(nombre, costo, precio);
        return repositorio.actualizarPlatillo(id, nombre, costo, precio);
    }

    public void cambiarDisponibilidadPlatillo(int id, boolean activo) throws SQLException {
        repositorio.cambiarDisponibilidadPlatillo(id, activo);
    }

    private static void validarFormulario(String nombre, BigDecimal costo, BigDecimal precio) {
        new Platillo(1, nombre, costo, precio);
    }

    public List<Pedido> listarPedidos() throws SQLException {
        return repositorio.listarPedidos();
    }

    public List<Pedido> listarPendientes() throws SQLException {
        return repositorio.listarPendientes();
    }

    public Pedido consultarPedido(int id) throws SQLException {
        return repositorio.buscarPedido(id);
    }

    public Pedido registrarPedido(Integer clienteId, Integer platilloId, Integer cantidad)
            throws SQLException {
        if (clienteId == null || clienteId <= 0
                || platilloId == null || platilloId <= 0
                || cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Cliente, platillo y cantidad deben ser positivos.");
        }
        Platillo platillo = repositorio.listarPlatillos().stream()
                .filter(p -> p.getId() == platilloId)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("El platillo no está disponible."));
        return repositorio.registrarPedido(clienteId, List.of(new DetallePedido(platillo, cantidad)));
    }

    public Pedido cambiarEstado(int pedidoId, EstadoPedido estado) throws SQLException {
        return repositorio.cambiarEstado(pedidoId, estado);
    }

    public Optional<Pedido> iniciarSiguiente() throws SQLException {
        return repositorio.iniciarSiguientePendiente();
    }
}
