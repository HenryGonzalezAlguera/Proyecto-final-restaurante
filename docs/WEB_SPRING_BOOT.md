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

## Fase 6 — Navegación y pantallas de consulta

- `/dashboard` presenta los cinco estados y el valor nominal de pedidos ENTREGADOS. **No representa pagos cobrados**.
- `/catalogo` presenta platillos activos con precio, costo de ingredientes y margen unitario. La vista es de lectura.
- `/historial` permite búsqueda por ID de pedido o cliente, filtro por estado y detalle desplegable por pedido.
- `/pedidos` conserva formularios y flujo de cambios de estados.
- Las rutas `/api/platillos` y `/api/pedidos` siguen devolviendo JSON; se muestran bajo «Desarrollo», separadas de las páginas de usuario.
- `AnaliticaRestauranteWeb` calcula el resumen desde pedidos persistidos; el ranking excluye pedidos cancelados. No se añade almacenamiento redundante ni tablas nuevas.
- `VistasRestauranteController` atiende las tres nuevas pantallas y presenta errores de conexión sin divulgar detalles de SQL.

Las pantallas usan el fragmento Thymeleaf `templates/fragmentos/navegacion.html` para compartir navegación. Los tests MockMvc comprueban HTML, filtros y fallos SQL; hay tests unitarios del cálculo de indicadores.

**Limitaciones:** historial sin auditoría cronológica ni paginación; catálogo sin CRUD web; sin autenticación, CSRF, pagos ni despliegue público. No presentar el valor de pedidos entregados como facturación.

## Fase 7 — Administración local del catálogo

- Crear platillos: nueva ruta `POST /catalogo/nuevo`, con identificador generado en MySQL.
- Editar nombre, costo y precio: `POST /catalogo/{id}/editar`.
- Desactivar o activar: `POST /catalogo/{id}/disponibilidad`, con `accion=desactivar` o `accion=activar`.
- Para los pedidos, la consulta `listarPlatillos()` continúa devolviendo **solo activos**. `listarCatalogoAdministracion()` incluye también los inactivos.
- La actualización del catálogo nunca modifica `pedido_detalles`; permanecen los nombres y precios históricos y se preservan las claves foráneas.
- Valores monetarios con `BigDecimal`, límite DECIMAL(12,2), ID positivo y nombre de máximo 150 caracteres.
- Antes de crear platillos, importar `database/03_migracion_catalogo.sql` desde phpMyAdmin; no se necesitan permisos ALTER para `restaurante_app`.
- Pruebas: MockMvc sobre formularios y MySQL real sobre altas, edición, desactivación, reactivación y conservación del historial.

**Restricción crítica:** los formularios aún no tienen autenticación, roles ni defensa CSRF. Mantener en `127.0.0.1` y no publicar el panel en Internet. La seguridad debe abordarse en una fase posterior antes de producción.

La migración 03 retira y recrea temporalmente la clave foránea `fk_detalles_platillo`. Antes de aplicarla en una base que ya contiene pedidos, realizar un respaldo y detener el servidor local.
