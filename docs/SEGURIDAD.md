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
