# Restaurante — Java / Maven

Sistema de restaurante **en desarrollo por fases**. Nació como un ejercicio de estructuras de datos y ahora incluye un modelo de dominio con validaciones y pruebas automáticas.

> **Estado actual:** demostración en consola, con datos de ejemplo almacenados únicamente en memoria. No es una aplicación web ni un sistema de ventas listo para producción.

## Funcionalidades verificables

- Catálogo de **platillos** con precios y costos en colones costarricenses (CRC).
- Registro de **empleados** con nombre y puesto.
- **Pedidos** con una o varias líneas, cantidades, subtotales y total.
- Estado inicial de cada pedido: `PENDIENTE`.
- Validaciones del dominio: identificadores positivos, textos no vacíos, montos válidos y pedidos con detalles.
- Manejo de importes mediante `BigDecimal`, evitando errores de coma flotante.
- Demostración de `Stack` (empleados) y `Queue` (pedidos).
- Pruebas JUnit 5 y validación automática con GitHub Actions.

**No implementado todavía:** modificar pedidos existentes, transiciones de estado, cancelaciones, cobros, inventario, base de datos, autenticación ni interfaz web.

## Tecnologías

- Java 17 o posterior
- Maven
- JUnit 5
- GitHub Actions

## Cómo ejecutar

Requisitos: JDK 17+ y Maven instalados.

Desde la raíz del repositorio:

```bash
mvn -f Restaurante/pom.xml clean verify
java -cp Restaurante/target/classes com.demo.restaurante.Restaurante
```

El primer comando compila, ejecuta las pruebas y genera los archivos del proyecto. El segundo ejecuta la demostración.

## Modelo de dominio

| Clase | Responsabilidad |
| --- | --- |
| `Platillo` | Nombre, precio, costo y margen bruto unitario |
| `Empleado` | Identificador, nombre y puesto |
| `DetallePedido` | Platillo, cantidad y subtotal; conserva una instantánea del precio |
| `Pedido` | Cliente, detalles, total y estado inicial |
| `EstadoPedido` | Estados previstos para la fase de gestión |

Para las reglas, decisiones de modelado y próximos pasos, consulta [docs/DOMINIO.md](docs/DOMINIO.md).

## Organización

```text
.
├── .github/workflows/java-maven.yml
├── docs/DOMINIO.md
├── README.md
└── Restaurante/
    ├── pom.xml
    └── src/
        ├── main/java/com/demo/restaurante/
        │   ├── Restaurante.java
        │   ├── Platillo.java
        │   ├── Empleado.java
        │   ├── Pedido.java
        │   ├── DetallePedido.java
        │   └── EstadoPedido.java
        └── test/java/com/demo/restaurante/
            ├── RestauranteTest.java
            └── ModeloDominioTest.java
```

## Evolución

- **Fase 1:** corregir errores de la demostración original, añadir README, JUnit y CI.
- **Fase 2:** definir entidades, validaciones, cantidades, precios y totales.
- **Fase 3 (pendiente):** gestionar ciclo de vida de pedidos, asignación de trabajo y operaciones de cobro.
- **Fase 4 (pendiente):** definir e implementar persistencia.
- **Fase 5 (pendiente):** desarrollar interfaz para el usuario.
- **Fase 6 (pendiente):** pruebas end-to-end, documentación visual y presentación de portafolio.

El propósito es demostrar evolución técnica sin atribuir funcionalidades que todavía no existen.
