# Fase 8 — Seguridad, cuentas y permisos

Este prototipo local ahora usa Spring Security 7 (mediante Spring Boot 4.1.2), sesiones HTTP, **BCrypt**, CSRF y autorizaciones. No existe registro público de usuarios ni contraseñas en GitHub.

## Modelo de acceso

| Acción | ADMIN | EMPLEADO | Anónimo |
| --- | --- | --- | --- |
| Iniciar/cerrar sesión | Sí | Sí | Solo login |
| Dashboard, pedidos e historial | Sí | Sí | No |
| Crear y procesar pedidos | Sí | Sí | No |
| Catálogo administrativo, alta, edición y disponibilidad | Sí | No | No |
| API de pedidos, lectura y operaciones POST | Sí | Sí | No |
| `GET /api/platillos` (incluye costos) | Sí | No | No |

Las validaciones de permisos ocurren **en el servidor**. Ocultar enlaces no sustituye esta protección.

## 1. Preparar una copia de seguridad

Detén Spring Boot (Ctrl+C) y exporta `restaurante_portafolio` desde phpMyAdmin. No modifiques otras bases ni usuarios de MySQL.

## 2. Importar migración

Con tu cuenta administradora de phpMyAdmin, importa **`database/04_usuarios_seguridad.sql`**. Esta migración solo crea la tabla `usuarios`. No cambia `pedidos`, `platillos` ni cuentas MySQL existentes.

La cuenta **MySQL** `restaurante_app` debe conservar `SELECT, INSERT, UPDATE` sobre `restaurante_portafolio` (no necesita permisos globales ni `ALTER`). Las cuentas de aplicación (`admin`, `empleado`) **son distintas** de `restaurante_app`: se autentican a través de Spring Security y se almacenan en la tabla `usuarios`.

## 3. Crear primera cuenta administradora

La primera vez, define estas variables únicamente en la misma ventana PowerShell donde ejecutarás el JAR. Conserva tu configuración previa `RESTAURANTE_DB_URL`, `RESTAURANTE_DB_USER` y `RESTAURANTE_DB_PASSWORD` (contraseña **del usuario MySQL** `restaurante_app`).

```powershell
$env:RESTAURANTE_ADMIN_USUARIO = "admin"
$segura = Read-Host "Nueva contraseña del administrador de Restaurante (mínimo 12 caracteres)" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
try {
    $env:RESTAURANTE_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
java -jar ".\RestauranteWeb\target\restaurante-web-1.0-SNAPSHOT.jar"
```

**El asistente no necesita conocer esa contraseña.** No la pegues en chats, archivos del proyecto ni capturas. No la reutilices de MySQL, de GitHub ni de otros sistemas.

Al arrancar, el inicializador crea la cuenta si el usuario no existe y guarda **solo el hash BCrypt** en MySQL. Reiniciar con las mismas variables **no cambia** la contraseña ni los roles de cuentas existentes. Si el esquema falta o el usuario de MySQL no tiene permisos, el arranque fallará: comprueba migración y conexión, sin revelar secretos.

Puedes crear opcionalmente un empleado en otra ejecución configurando `RESTAURANTE_EMPLEADO_USUARIO` y `RESTAURANTE_EMPLEADO_PASSWORD` de la misma forma (mínimo 12 caracteres). La cuenta por defecto es `empleado` si no indicas otro nombre. No hay formulario de autorregistro, ni creación arbitraria de administradores desde la web.

Una vez verificado el usuario, **detén la aplicación y abre otra sesión de PowerShell** configurando solo las variables de MySQL para continuar; ya no necesitas mantener la contraseña de bootstrap. Evita historiales de terminal o variables permanentes con contraseñas.

Consulta desde phpMyAdmin sin mostrar hashes:

```sql
SELECT usuario, rol, activo FROM restaurante_portafolio.usuarios ORDER BY id;
```

## 4. Inicio y cierre de sesión

Desde PowerShell, inicia el JAR compilado:

```powershell
java -jar ".\RestauranteWeb\target\restaurante-web-1.0-SNAPSHOT.jar"
```

Abre `http://127.0.0.1:8085/login`; después del login verás Dashboard. Usa el botón **Cerrar sesión** de la barra lateral. No hay usuarios de ejemplo con claves predeterminadas.

## 5. CSRF y API

Los formularios Thymeleaf envían el token de CSRF de Spring Security. Los POST sin token válido responden **HTTP 403**, incluso si la sesión está autenticada. Las rutas JSON requieren una sesión autenticada; para POST de API también debes pasar el token de CSRF de la sesión en un encabezado `X-CSRF-TOKEN` o parámetro `_csrf`. Un `curl` sin login ni token ya no puede cambiar pedidos.

Los endpoints de consulta siguen devolviendo JSON cuando el usuario tiene permiso; los navegadores no autenticados pueden ser redirigidos a `/login`.

## 6. Alcance de seguridad

- No desplegar aún en Internet. El servidor sigue escuchando `127.0.0.1:8085`.
- En producción faltarían HTTPS, cookies `Secure`, gestión de cuentas, recuperación de contraseña, bloqueo/rate limiting de intentos, auditoría de cambios, rotación de secretos, revisión de dependencias y pruebas de penetración.
- El esquema de sesión y CSRF supone navegador/cliente con cookies; los clientes API externos necesitarán un mecanismo de autenticación adecuado, nunca desactivar CSRF sin diseñarlo.
- No hay pagos ni datos sensibles reales. La tabla `usuarios` no comparte credenciales con las cuentas de MySQL.

## Fase 9A — Administración de empleados

Desde la pantalla **Usuarios** (`/usuarios`), un administrador puede consultar los empleados existentes y crear cuentas `EMPLEADO` con una contraseña temporal propia.

- Requiere haber importado `database/04_usuarios_seguridad.sql` y haber iniciado sesión con `ADMIN`.
- `GET /usuarios` y `POST /usuarios/empleados` están protegidos en el filtro de Spring Security con el rol `ADMIN`.
- Los formularios requieren CSRF y los secretos no se imprimen en logs, vistas ni URLs.
- La validación rechaza nombres fuera de `[a-zA-Z0-9_.-]{3,64}`, contraseñas de menos de 12 caracteres o mayores a 72 bytes UTF-8 y cuentas duplicadas.
- Esta pantalla **solo** crea empleados; no puede crear ni modificar administradores.
- Todavía no hay revocación inmediata de sesiones existentes, restablecimiento de claves ni auditoría. Estos aspectos deben cubrirse antes de desplegar públicamente.

No se necesita una migración adicional: se reutiliza la tabla `usuarios` de la fase 8.

## Fase 9B — Activación, desactivación y restablecimiento de contraseñas

El administrador gestiona **solo empleados** desde `/usuarios`. No se pueden desactivar, reactivar ni cambiar contraseñas de cuentas `ADMIN` mediante esta pantalla.

- `POST /usuarios/empleados/{id}/estado` acepta exclusivamente `accion=activar` o `accion=desactivar`.
- `POST /usuarios/empleados/{id}/clave` recibe `password` y `confirmacion`; la contraseña debe tener al menos 12 caracteres, máximo 72 bytes UTF-8 y ambos valores deben coincidir.
- Los controles requieren sesión de `ADMIN` y token CSRF. No se exponen hashes, contraseñas ni mensajes internos de SQL al navegador.
- Desactivar impide **nuevos** accesos. El filtro de vigencia comprueba cada petición de una sesión real contra la tabla `usuarios`; si detecta cuenta inactiva, hash cambiado o rol cambiado, invalida la sesión y redirige a `/login?sesion=revocada`. Las rutas `/api/` devuelven HTTP 401 en ese caso.
- Si MySQL falla durante la comprobación de vigencia, se responde HTTP 503 (fallo cerrado), sin ejecutar la acción solicitada.
- Restablecer una clave **no reactiva** automáticamente una cuenta desactivada.
- Se reutiliza la tabla `usuarios`: no es necesaria ninguna nueva migración SQL.

### Prueba local sugerida

1. Inicia sesión como `admin` y entra en `/usuarios`.
2. Crea un empleado de prueba y abre una sesión como ese empleado en otro navegador o ventana privada.
3. Desde la sesión de administrador, desactiva al empleado y prueba actualizar `/pedidos` en su sesión: deberá solicitar un nuevo login.
4. Reactívalo, inicia sesión de nuevo con su contraseña anterior, restablece la clave desde admin y actualiza la sesión del empleado: deberá cerrarse.
5. Confirma que la contraseña antigua falla y la nueva permite el ingreso. No compartas las contraseñas con terceros ni las captures en pantallas.

**Límites:** la revocación se aplica en la siguiente petición autenticada, no detiene una operación que ya esté ejecutándose. Es una comprobación JDBC por petición, adecuada para la demo local pero no optimizada para grandes cargas. Para Internet todavía faltan HTTPS, limitación de intentos de login, auditoría, mecanismos de recuperación verificada y pruebas de seguridad.

## Fase 9C — Auditoría de operaciones

Importa `database/05_auditoria_operaciones.sql` en `restaurante_portafolio` mediante phpMyAdmin después de hacer copia de seguridad y detener Spring Boot. El script crea únicamente `auditoria_operaciones`; el usuario técnico `restaurante_app` requiere `SELECT` e `INSERT` sobre esa tabla. No son necesarios permisos `DELETE` ni `UPDATE` en la bitácora.

La ruta `/auditoria` (solo `ADMIN`) muestra eventos paginados de pedidos, catálogo y cuentas. Guarda fecha/hora de MySQL, actor autenticado, acción de una lista cerrada, entidad, referencia y detalle limitado a estados. Nunca almacena contraseñas, hashes, tokens, precios, datos de clientes ni texto arbitrario suministrado por el usuario. No reconstruye operaciones previas a su instalación.

**Límite de integridad:** el evento se escribe **después** de completar la operación y en otra conexión JDBC. Una interrupción o error de conexión puede dejar operaciones exitosas sin un evento: esta bitácora es trazabilidad operativa, NO auditoría atómica ni forense. Las vistas muestran una advertencia si el registro falla; el servidor también registra una advertencia genérica sin información sensible. El POST de creación por API expone `X-Audit-Status: recorded|missing`; otros cambios API se notifican en logs.

### Prueba manual
1. Importa migración 05 con la aplicación detenida.
2. Abre `/auditoria` como admin y crea un pedido, avanza su estado, edita un platillo y cambia el estado de un empleado.
3. Verifica actor y referencia en los nuevos eventos. La cuenta `EMPLEADO` no puede consultar `/auditoria`.

Para garantías más fuertes hará falta insertar el evento dentro de la **misma transacción JDBC** que el cambio de negocio, más controles de retención e inmutabilidad.
