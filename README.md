# Restaurante — Java, Spring Boot y MySQL

Aplicación de demostración para portafolio que evolucionó desde una práctica de estructuras de datos hasta una **interfaz web de pedidos** y **API REST local**, reutilizando el modelo de dominio original y la persistencia JDBC.

> **Estado: prototipo local.** Sin autenticación, pagos, facturación ni datos reales. Por seguridad el servidor web se enlaza a `127.0.0.1` de forma predeterminada. **No exponer a Internet** sin incorporar seguridad, permisos y pruebas adicionales.

## Qué permite hacer

- Visualizar el catálogo de platillos, precios y pedidos.
- Registrar un pedido desde el navegador: cliente, platillo y cantidad.
- Consultar estado, precio total y número de pedidos pendientes.
- Procesar pendientes **FIFO**, marcar platos listos y entregar.
- Cancelar pedidos que aún no están en preparación.
- Consultar y gestionar pedidos mediante una API HTTP JSON local.
- Conservar los precios históricos y la integridad de transacciones en MySQL.

Las reglas de validación y transición se implementan una sola vez en el módulo Java y se **reutilizan** desde Spring Boot.

## Arquitectura

```text
.
├── pom.xml                       # Reactor Maven: dos módulos
├── Restaurante/                  # Dominio, gestor Java, JDBC y pruebas
├── RestauranteWeb/               # Spring Boot MVC, Thymeleaf y API JSON
│   ├── pom.xml
│   ├── src/main/java/com/demo/restaurante/web/
│   ├── src/main/resources/templates/pedidos.html
│   ├── src/main/resources/static/css/restaurante.css
│   └── src/test/java/com/demo/restaurante/web/
├── database/                     # Esquema SQL y guía XAMPP
└── docs/
```

| Tecnología | Uso |
| --- | --- |
| Java 17 | Modelo y lógica de negocio |
| Spring Boot 4.1.2 | Aplicación web y API REST |
| Spring MVC y Thymeleaf | Controladores y vistas HTML |
| MySQL 8 + JDBC | Persistencia de datos |
| Maven | Compilación de los dos módulos |
| JUnit 5, MockMvc y GitHub Actions | Pruebas unitarias, SQL y HTTP |

## Compilar y probar

Requisitos: Java 17+, Maven 3.6.3+ y, para la modalidad persistente, MySQL 8.

```bash
mvn -f pom.xml clean verify
```

La comprobación de MySQL se ejecuta automáticamente en CI contra un contenedor aislado; localmente se omite si no se definen las variables `RESTAURANTE_TEST_DB_*`.

## Ejecutar la aplicación web en XAMPP

1. Abre [database/README.md](database/README.md) e importa `database/01_esquema.sql` en una base **independiente** llamada `restaurante_portafolio`.
2. Importa también `database/02_datos_demo.sql` para disponer de cinco platillos y tres empleados ficticios (sin sobrescribir registros existentes).
3. Crea un usuario MySQL específico `restaurante_app` con acceso a esa base; no reutilices las contraseñas de otros proyectos.
4. Configura las variables `RESTAURANTE_DB_URL`, `RESTAURANTE_DB_USER`, `RESTAURANTE_DB_PASSWORD` solo en tu máquina, como indica la guía.
5. Desde la raíz ejecuta:

```bash
mvn -f pom.xml clean package
java -jar RestauranteWeb/target/restaurante-web-1.0-SNAPSHOT.jar
```

6. Abre **http://127.0.0.1:8085/pedidos**.

La interfaz informa si faltan credenciales o esquema. La aplicación **no crea automáticamente la base ni las tablas**. La inicialización de catálogo debe hacerse en MySQL o con la demostración JDBC explicada en `database/README.md`. Para el módulo original de consola:

```bash
java -cp Restaurante/target/classes com.demo.restaurante.Restaurante
```

## API REST de demostración

| Método | Ruta | Acción |
| --- | --- | --- |
| GET | `/api/platillos` | Catálogo |
| GET | `/api/pedidos` | Lista de pedidos |
| GET | `/api/pedidos/{id}` | Consulta de pedido |
| POST | `/api/pedidos` | Crear pedido |
| POST | `/api/pedidos/{id}/estado` | Avanzar estado |
| POST | `/api/pedidos/siguiente` | Preparar siguiente (FIFO) |

Cuerpo para crear un pedido:

```json
{"clienteId": 1, "platilloId": 1, "cantidad": 2}
```

Cuerpo para cambiar estado:

```json
{"estado": "LISTO"}
```

No todos los cambios de estado están permitidos; consulta [docs/FLUJO_PEDIDOS.md](docs/FLUJO_PEDIDOS.md).

## Limitaciones y seguridad

- Prototipo local **sin sistema de acceso** ni protección CSRF: no desplegarlo en un servidor público.
- El formulario web crea **un tipo de platillo por pedido**; la lógica Java y la API de repositorio ya admiten múltiples detalles y se ampliará la interfaz posteriormente.
- No hay caja, pagos, IVA, inventario, sesiones de usuario, facturación ni reservas.
- No se guardan credenciales reales en GitHub.
- La demostración de consola y el componente web son independientes: uno utiliza datos ficticios en memoria y el otro consulta MySQL.
- Las pruebas HTTP usan servicios simulados y las de persistencia usan una base MySQL temporal en GitHub Actions.

## Historia de evolución

1. Correcciones del ejercicio inicial, documentación y CI.
2. Modelo del dominio y validaciones.
3. Ciclo de vida de pedidos y FIFO.
4. Persistencia transaccional con JDBC y pruebas de integración.
5. Aplicación web y API con Spring Boot (**fase actual**).
6. End-to-end, control de acceso, mejoras de interfaz y presentación final (**pendiente**).

Para empezar consulta también [docs/WEB_SPRING_BOOT.md](docs/WEB_SPRING_BOOT.md) y [database/README.md](database/README.md).
