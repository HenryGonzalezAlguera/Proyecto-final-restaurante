# Fase 2 — Modelo de dominio de Restaurante

## Alcance

Esta fase transforma la demostración inicial en un modelo de negocio **en memoria**, sin introducir todavía Spring Boot, base de datos ni interfaces gráficas. Las estructuras `Stack` y `Queue` siguen apareciendo en `Restaurante.main` para reflejar el origen académico.

## Entidades y responsabilidades

- **Platillo:** `id`, `nombre`, `costoIngredientes`, `precio`; expone margen bruto unitario. Sustituye el uso incorrecto de la clase anterior `Proveedor` para representar comidas.
- **Empleado:** `id`, `nombre`, `puesto` con validaciones básicas.
- **DetallePedido:** instantánea del ID, nombre y precio unitario del platillo más cantidad. `subtotal = precioUnitario × cantidad`.
- **Pedido:** `id`, `clienteId`, lista no vacía de detalles y estado inicial `PENDIENTE`. `total = suma(subtotales)`. Sustituye la antigua `Orden`, que solo registraba el identificador del cliente.
- **EstadoPedido:** `PENDIENTE`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`. Únicamente se asigna el estado inicial en esta fase; las transiciones deben implementar reglas en fase 3.

## Reglas de validación implementadas

1. Todos los identificadores de dominio deben ser enteros positivos.
2. Los nombres y puestos no pueden ser nulos ni blancos.
3. El precio debe ser mayor que cero; el costo de ingredientes puede ser cero, pero no negativo.
4. Los montos usan `BigDecimal` y como máximo dos decimales, sin redondeos implícitos.
5. La cantidad de cada detalle debe ser positiva.
6. Todo pedido debe tener al menos un detalle válido.
7. Los pedidos guardan una copia no modificable de la lista de detalles.
8. El importe del detalle se congela al crear el detalle: una futura actualización de catálogo no debe cambiar el precio histórico de un pedido.

Los importes de la demostración se expresan en **CRC**. No hay cálculo de IVA, promociones, cobros ni pagos todavía: el total es el subtotal comercial de los platillos.

## Ejemplo de datos demostrativos

| Pedido | Detalles | Total CRC | Estado |
| --- | --- | ---: | --- |
| 1 | 2 × Arroz con Pollo (₡4 000) | ₡8 000 | PENDIENTE |
| 2 | 1 × La Carnita Roja (₡6 000), 1 × Arroz con Pollo (₡4 000) | ₡10 000 | PENDIENTE |
| 3 | 2 × La Carnita Roja (₡6 000) | ₡12 000 | PENDIENTE |

Todos los datos son ficticios; no se almacenan entre ejecuciones.

## Fuera del alcance de fase 2

- Alta, modificación y cancelación interactivas de pedidos.
- Reglas de transiciones y responsables de preparación.
- Persistencia y concurrencia.
- Cobros, facturación e impuestos.
- API, interfaz web o móvil.

## Próxima fase

Crear operaciones explícitas para registrar pedidos y actualizar su estado con reglas de transición. Probar intentos inválidos, cancelaciones y consistencia del cálculo. No agregar persistencia ni pagos antes de definir requisitos.
