package com.demo.restaurante.web;

import com.demo.restaurante.DetallePedido;
import com.demo.restaurante.EstadoPedido;
import com.demo.restaurante.Pedido;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Estadísticas descriptivas de los pedidos actualmente almacenados.
 * No son ventas cobradas ni ingresos contables: no existe módulo de pagos.
 */
@Service
public class AnaliticaRestauranteWeb {

    public record ProductoSolicitado(String nombre, long unidades) { }

    public record Resumen(long total, long pendientes, long preparacion,
                          long listos, long entregados, long cancelados,
                          BigDecimal valorEntregado, List<ProductoSolicitado> destacados) { }

    public Resumen resumir(List<Pedido> pedidos) {
        long pendientes = 0, preparacion = 0, listos = 0, entregados = 0, cancelados = 0;
        BigDecimal valorEntregado = new BigDecimal("0.00");
        Map<String, Long> unidades = new HashMap<>();

        for (Pedido pedido : pedidos) {
            switch (pedido.getEstado()) {
                case PENDIENTE -> pendientes++;
                case EN_PREPARACION -> preparacion++;
                case LISTO -> listos++;
                case ENTREGADO -> {
                    entregados++;
                    valorEntregado = valorEntregado.add(pedido.getTotal());
                }
                case CANCELADO -> cancelados++;
            }
            if (pedido.getEstado() != EstadoPedido.CANCELADO) {
                for (DetallePedido detalle : pedido.getDetalles()) {
                    unidades.merge(detalle.getNombrePlatillo(), (long) detalle.getCantidad(), Long::sum);
                }
            }
        }

        List<ProductoSolicitado> destacados = unidades.entrySet().stream()
                .map(item -> new ProductoSolicitado(item.getKey(), item.getValue()))
                .sorted(Comparator.comparingLong(ProductoSolicitado::unidades).reversed()
                        .thenComparing(ProductoSolicitado::nombre))
                .limit(5)
                .toList();

        return new Resumen(pedidos.size(), pendientes, preparacion, listos, entregados,
                cancelados, valorEntregado, destacados);
    }

    public List<Pedido> filtrar(List<Pedido> pedidos, String estado, String busqueda) {
        EstadoPedido estadoSolicitado = null;
        if (estado != null && !estado.isBlank()) {
            try {
                estadoSolicitado = EstadoPedido.valueOf(estado);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("El filtro de estado no es válido.");
            }
        }

        String texto = busqueda == null ? "" : busqueda.trim();
        List<Pedido> resultado = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            boolean coincideEstado = estadoSolicitado == null || pedido.getEstado() == estadoSolicitado;
            boolean coincideTexto = texto.isBlank()
                    || Integer.toString(pedido.getId()).contains(texto)
                    || Integer.toString(pedido.getClienteId()).contains(texto);
            if (coincideEstado && coincideTexto) {
                resultado.add(pedido);
            }
        }
        resultado.sort(Comparator.comparingInt(Pedido::getId).reversed());
        return List.copyOf(resultado);
    }
}
