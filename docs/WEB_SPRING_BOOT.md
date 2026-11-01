# Fase 5 — Spring Boot MVC y API REST

## Decisión arquitectónica

Se conserva **sin cambios de tecnología** el módulo `Restaurante`, ya validado con Maven, JUnit y MySQL JDBC. La fase 5 añade `RestauranteWeb`, que depende del artefacto Maven `com.demo:Restaurante:1.0-SNAPSHOT`.

La raíz `pom.xml` utiliza un **reactor Maven** para compilar primero el módulo de dominio y luego la aplicación Spring Boot. No se trasladan reglas SQL ni transiciones de estados a las vistas o controladores.

## Componentes

| Archivo | Responsabilidad |
| --- | --- |
| `RestauranteWebApplication` | Entrada Spring Boot |
| `RestauranteConfiguracion` | Bean JDBC con conexión por variables de entorno |
| `ServicioRestauranteWeb` | Casos de uso que reutilizan `RepositorioRestauranteJdbc` |
| `PedidosWebController` | Vista Thymeleaf y formularios |
| `PedidosApiController` | Endpoints JSON para consumo local |
| `ApiErroresHandler` | Errores HTTP sin exponer SQL ni contraseñas |
| `pedidos.html` | Panel con catálogo, captura y cambio de estados |
| `restaurante.css` | Diseño adaptable, sin librerías frontend |

## Inicio local y acceso

- `server.address=127.0.0.1` por defecto: **solo el equipo local**.
- `server.port=8085` por defecto: puerto configurable con `RESTAURANTE_WEB_PORT`.
- `RESTAURANTE_DB_URL`, `RESTAURANTE_DB_USER` y `RESTAURANTE_DB_PASSWORD` se leen **en tiempo de conexión**.
- Si faltan credenciales o la base, el panel mostrará un mensaje; no se conecta a ninguna base ajena automáticamente.

La guía [database/README.md](../database/README.md) explica cómo configurar una base independiente en XAMPP.

## Reglas de negocio conservadas

- Los pedidos registran instantánea de nombres y precios.
- Cantidades e IDs deben ser positivos.
- Los cambios de estado usan la misma máquina definida en `Pedido`.
- La selección FIFO se ejecuta desde el repositorio transaccional JDBC.
- Los detalles nunca se reprecian con datos actuales del menú.

## Alcance real

El panel permite registrar un platillo y su cantidad por pedido, cambiar estados y consultar totales; la API expone operaciones equivalentes. No existen autenticación, autorización, CSRF, inventario, pagos, facturación, registros de clientes ni despliegue público. El siguiente ciclo deberá abordar controles de acceso y pruebas end-to-end antes de ofrecer el servicio en una red.

## Pruebas

`mvn -f pom.xml clean verify` construye y ejecuta los tests de ambos módulos. GitHub Actions levanta MySQL 8.4 de CI para las pruebas JDBC y MockMvc verifica las rutas web sin manipular la base de datos real del usuario.
