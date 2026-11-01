# Restaurante — Gestión de pedidos con Java

Proyecto de portafolio en **Java 17 y Maven** que evolucionó a partir de un ejercicio académico de estructuras de datos. Actualmente demuestra un **modelo de negocio en memoria** con pedidos, catálogo, validaciones, flujo de estados y procesamiento por orden de llegada.

> **Estado actual: aplicación de consola demostrativa.** No tiene base de datos, interfaz web ni procesamiento real de pagos. Los datos son ficticios y se pierden al terminar la ejecución.

## Funcionalidades implementadas

- Catálogo de platillos con precio, costo y margen bruto unitario en CRC.
- Empleados con identificador, nombre y puesto.
- Registro de pedidos con identificador consecutivo, cliente y uno o varios detalles.
- Subtotales y total calculados exactamente con `BigDecimal`.
- Consulta y listado de pedidos registrados.
- Cola de pendientes **FIFO**, que inicia primero el pedido más antiguo todavía pendiente.
- Estados de pedido con transiciones controladas y cancelación antes de preparar.
- Detalles inmutables y conservación de precios y totales durante las transiciones.
- Validación de entradas, pruebas JUnit 5 y CI en GitHub Actions.

## Flujo de pedidos

```text
PENDIENTE ───> EN_PREPARACION ───> LISTO ───> ENTREGADO
     │
     └────────> CANCELADO
```

Solo se puede cancelar un pedido **PENDIENTE**. Los estados `ENTREGADO` y `CANCELADO` son terminales. Un intento de transición no permitida produce un error y conserva el estado anterior.

La política y los casos límite se detallan en [docs/FLUJO_PEDIDOS.md](docs/FLUJO_PEDIDOS.md).

## Tecnologías

Java 17+, Maven, JUnit 5 y GitHub Actions. Los datos se gestionan en memoria usando `LinkedHashMap` y `ArrayDeque`. La aplicación de demostración conserva el uso de `Stack` para empleados.

## Ejecutar y probar

Requisitos: JDK 17+ y Apache Maven.

Desde la raíz del repositorio:

```bash
mvn -f Restaurante/pom.xml clean verify
java -cp Restaurante/target/classes com.demo.restaurante.Restaurante
```

El primer comando compila y ejecuta las pruebas. El segundo demuestra tres pedidos: uno entregado, uno pendiente y uno cancelado.

La consola muestra información; el registro y las operaciones de pedidos se encuentran en la clase `GestorPedidos`, que todavía no posee un menú interactivo.

## Componentes

| Clase | Responsabilidad |
| --- | --- |
| `Platillo` | Datos del menú, precios y costos |
| `Empleado` | Datos y validación de empleados |
| `DetallePedido` | Línea con cantidad y precio congelado |
| `Pedido` | Estado, detalles, total y transiciones válidas |
| `EstadoPedido` | Estados del ciclo de vida |
| `GestorPedidos` | Registro, consulta, cancelación y cola FIFO |
| `Restaurante` | Ejemplo ejecutable en consola |

Consulta también [docs/DOMINIO.md](docs/DOMINIO.md) para el modelo.

## Evolución por fases

| Fase | Estado |
| --- | --- |
| 1. Corregir errores de la práctica, documentar y configurar CI | Completada |
| 2. Introducir entidades y reglas de dominio con pruebas | Completada |
| 3. Registrar y procesar pedidos, validar estados y cancelación | Implementada en rama de revisión |
| 4. Persistencia y diseño de almacenamiento | Pendiente |
| 5. API/interfaz web y operaciones de usuario | Pendiente |
| 6. Pruebas finales, documentación visual y publicación | Pendiente |

**Límites explícitos:** no existen usuarios autenticados, pagos, impuestos, inventario, facturación, sincronización multiusuario, conexión MySQL ni API HTTP. Las operaciones actuales son locales, no persistentes y no son seguras para acceso concurrente. Las fases futuras se definirán sobre esta base verificada.
