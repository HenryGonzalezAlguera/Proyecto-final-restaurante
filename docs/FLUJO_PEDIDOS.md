# Gestión y ciclo de vida de pedidos (origen: fase 3)

> Este documento se centra en la **lógica del dominio** creada en la fase 3. Su máquina de estados continúa vigente en la aplicación web actual. Consulta [ARQUITECTURA.md](ARQUITECTURA.md) para el flujo entre Spring Boot y JDBC.

## Objetivo y alcance

Agregar operaciones reales en memoria sobre el modelo de la fase 2, sin introducir todavía una interfaz interactiva, base de datos ni integración de cobros. La demostración de consola registra tres pedidos y muestra su avance.

## API del gestor

| Operación | Comportamiento |
| --- | --- |
| `registrarPedido(clienteId, detalles)` | Valida datos, asigna ID consecutivo y añade el pedido a la cola pendiente |
| `buscarPedido(pedidoId)` | Devuelve pedido actual o falla si el ID no existe |
| `listarPedidos()` | Devuelve todos los pedidos por orden de registro |
| `listarPendientes()` | Devuelve una lista no modificable en orden de llegada |
| `iniciarSiguientePendiente()` | Inicia el primer pendiente (FIFO); devuelve `Optional.empty()` si no hay |
| `cambiarEstado(pedidoId, estado)` | Aplica solo transiciones válidas |
| `cancelarPedido(pedidoId)` | Cancela únicamente pedidos pendientes |

## Máquina de estados

```text
PENDIENTE --iniciar--> EN_PREPARACION --preparar--> LISTO --entregar--> ENTREGADO
    |
    +--cancelar--> CANCELADO
```

| Estado original | Transiciones permitidas |
| --- | --- |
| PENDIENTE | EN_PREPARACION, CANCELADO |
| EN_PREPARACION | LISTO |
| LISTO | ENTREGADO |
| ENTREGADO | Ninguna |
| CANCELADO | Ninguna |

Las transiciones al mismo estado también se rechazan. **Decisión de negocio de esta fase:** no se puede cancelar una vez iniciada la preparación; esta política podrá revisarse si cambian los requisitos, pero no se omite silenciosamente.

## Consistencia y comportamiento de errores

- `Pedido` sigue siendo inmutable: cambiar de estado produce una nueva instancia conservando identificadores, detalles y total.
- `GestorPedidos` mantiene la última versión para cada ID.
- La cola FIFO contiene exclusivamente IDs de pedidos `PENDIENTE`. Al iniciar o cancelar un pendiente, se retira de la cola.
- Una transición no permitida lanza `IllegalStateException` **antes** de modificar el registro o la cola.
- La ausencia del pedido solicitado lanza `NoSuchElementException`.
- Un pedido inválido no consume identificadores ni altera la cola.
- Las colecciones expuestas al exterior son no modificables.
- Los montos expresados en CRC usan `BigDecimal`; los importes originales del pedido no cambian.

## Escenario de la demostración

| Pedido | Composición | Total CRC | Estado al terminar |
| --- | --- | ---: | --- |
| #1 | 2 × Arroz con Pollo | 8000.00 | ENTREGADO |
| #2 | 1 × La Carnita Roja + 1 × Arroz con Pollo | 10000.00 | PENDIENTE |
| #3 | 2 × La Carnita Roja | 12000.00 | CANCELADO |

La demostración cancela el tercero, toma el primero de la cola y lo hace avanzar hasta `ENTREGADO`.

## Pruebas de regresión

`GestorPedidosTest` valida registro y secuencia de IDs, orden FIFO, cancelación, ausencia de pedidos, estados terminales, saltos inválidos, colecciones no modificables y conservación de totales. `RestauranteTest` verifica la salida de consola. `ModeloDominioTest` conserva las comprobaciones de la fase 2.

La compilación y pruebas se ejecutan en GitHub Actions con `mvn -B -ntp -f Restaurante/pom.xml clean verify`.

## Alcance que se difiere

- Persistencia y operaciones multiusuario/concurrentes.
- Interfaz de creación de pedidos y gestión de catálogo.
- Reservas, asignación de mesas o mesas físicas.
- Pagos y cobros: requieren un modelo separado de transacciones, métodos de pago y control de duplicados; no deben confundirse con `ENTREGADO`.
- Impuestos, inventario y facturación.

Se trata de un prototipo de lógica de negocio, no de un POS comercial.
