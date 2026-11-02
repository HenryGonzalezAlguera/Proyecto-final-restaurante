# Modelo de dominio de Restaurante

> **Alcance del documento:** describe el modelo Java original de consola. La versión web actual **sí** cuenta con MySQL, Spring Boot, API REST y autenticación. Consulta [ARQUITECTURA.md](ARQUITECTURA.md) para el sistema completo.

## Objetivo

Proyecto demostrativo de gestión de restaurante con reglas de negocio en memoria. La fase 2 definió las entidades y la fase 3 añadió operaciones de pedidos y reglas del ciclo de vida.

## Entidades

- **Platillo:** `id`, `nombre`, `costoIngredientes` y `precio`. Calcula margen bruto unitario.
- **Empleado:** `id`, `nombre` y `puesto`, con validaciones.
- **DetallePedido:** captura ID, nombre y precio del platillo junto con la cantidad. El subtotal es `precioUnitario × cantidad`.
- **Pedido:** ID, cliente, detalles no vacíos, total y estado; inicia en `PENDIENTE`. Cada transición válida devuelve una nueva instancia.
- **EstadoPedido:** `PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO` y `CANCELADO`.
- **GestorPedidos:** registro secuencial, consulta, listado, procesamiento FIFO, cambios de estado y cancelación.

## Reglas de validación

1. Los identificadores de entidades deben ser positivos.
2. Nombres y puestos no pueden estar vacíos.
3. El precio debe ser positivo y el costo de ingredientes no negativo.
4. Los montos son `BigDecimal` con dos decimales, sin redondeo implícito.
5. Las cantidades de cada detalle deben ser positivas.
6. Los pedidos deben tener al menos un detalle válido.
7. Cada pedido conserva una copia no modificable de sus detalles.
8. El precio de un detalle queda capturado al crearse; una modificación posterior del catálogo no debe afectar pedidos existentes.
9. Los cambios de estado deben respetar el flujo documentado y no modifican el total.
10. Solo los pedidos pendientes permanecen en la cola FIFO.

## Ciclo de vida

Consulta [FLUJO_PEDIDOS.md](FLUJO_PEDIDOS.md) para transiciones, errores y ejemplos.

## Limitaciones

Esta fase es una simulación en memoria. No incluye autenticación, cobros, impuestos, persistencia, multiusuario, API web ni operaciones de concurrencia.
