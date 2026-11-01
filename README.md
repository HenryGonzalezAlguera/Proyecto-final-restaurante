# Restaurante — Java, MySQL y gestión de pedidos

Sistema de restaurante **en desarrollo por fases**. Se inició como ejercicio académico de estructuras de datos y ahora dispone de reglas de negocio, validaciones, pruebas JUnit y un repositorio JDBC para MySQL 8.

> **Estado:** demostración de consola con lógica en memoria y **demostración optativa de persistencia MySQL**. No es un sistema POS comercial, no hay interfaz web ni pagos o facturas reales. Se utilizan datos ficticios.

## Funcionalidades reales

- Catálogo de platillos con costos, precios y margen unitario en CRC.
- Empleados con nombre, identificador y puesto.
- Pedidos con múltiples detalles, cantidades, subtotales y total calculado con `BigDecimal`.
- Cola FIFO de pedidos pendientes.
- Estados: `PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO`, o `PENDIENTE → CANCELADO`.
- Registro, consulta, cancelación y actualizaciones de estado bajo reglas explícitas.
- **Persistencia MySQL opcional:** guarda y recupera catálogo, empleados, cabeceras y detalles de pedidos.
- Transacciones JDBC para evitar pedidos incompletos y bloqueos de fila para cambios de estado.
- Instantáneas de precios históricos: modificar el menú no cambia el precio de un pedido previo.
- Pruebas unitarias JUnit 5 y pruebas de integración real con MySQL en GitHub Actions.

## Arquitectura actual

| Componente | Responsabilidad |
| --- | --- |
| `Platillo`, `Empleado`, `DetallePedido`, `Pedido` | Modelo de dominio |
| `EstadoPedido` | Estados del ciclo de vida |
| `GestorPedidos` | Gestión **en memoria**, útil para aprender y probar reglas |
| `RepositorioRestauranteJdbc` | Gestión **persistente** con JDBC y transacciones MySQL |
| `ConexionRestaurante` | Acceso con variables de entorno, sin claves embebidas |
| `Restaurante` | Demostración independiente solo en memoria |
| `RestauranteMysqlDemo` | Demostración explícita que escribe y lee MySQL |

No utiliza Spring Boot todavía: la interfaz web/API será una fase independiente.

## Requisitos

- JDK 17 o superior
- Apache Maven
- MySQL 8 para la modalidad persistente

## Ejecutar pruebas y demostración en memoria

Desde la raíz del repositorio:

```bash
mvn -f Restaurante/pom.xml clean verify
java -cp Restaurante/target/classes com.demo.restaurante.Restaurante
```

Las pruebas JDBC se omiten fuera de un entorno de pruebas MySQL explícito. En GitHub Actions se ejecutan contra una base de CI aislada.

## Ejecutar la demostración MySQL

1. Crea una base independiente siguiendo **[database/README.md](database/README.md)**.
2. Importa el archivo `database/01_esquema.sql`.
3. Configura `RESTAURANTE_DB_URL`, `RESTAURANTE_DB_USER` y `RESTAURANTE_DB_PASSWORD` **solo en tu equipo**.
4. Ejecuta:

```bash
mvn -f Restaurante/pom.xml clean verify
mvn -f Restaurante/pom.xml exec:java -Dexec.mainClass=com.demo.restaurante.RestauranteMysqlDemo -Dexec.args=--demo
```

**Advertencia:** la demostración `--demo` inserta un pedido ficticio en la base configurada cada vez que se ejecuta. No apunta a ninguna base de otra aplicación.

## Seguridad y límites

- No subas contraseñas, URLs con secretos ni archivos de configuración locales.
- No reutilices `root` ni el usuario de otros proyectos; usa una cuenta dedicada.
- La persistencia no implementa autenticación, autorización ni acceso concurrente escalable.
- La base de CI usa credenciales **ficticias y efímeras** para su propio contenedor.
- `ENTREGADO` no equivale a `PAGADO`. No hay transacciones de cobro, inventario, IVA ni facturación.
- La demostración es local y no requiere desplegar MySQL en Internet.

## Fases

| Fase | Resultado |
| --- | --- |
| 1 | Correcciones iniciales, JUnit, GitHub Actions |
| 2 | Modelo de dominio, precios y validaciones |
| 3 | Registro y procesamiento FIFO, estados, cancelación |
| 4 | Persistencia MySQL con JDBC y pruebas de integración |
| 5 (pendiente) | API e interfaz web con Spring Boot |
| 6 (pendiente) | Validación end-to-end, documentación y capturas |

Ver [docs/DOMINIO.md](docs/DOMINIO.md), [docs/FLUJO_PEDIDOS.md](docs/FLUJO_PEDIDOS.md) y [database/README.md](database/README.md).
