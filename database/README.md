# Base de datos de Restaurante

## Aislamiento del proyecto

El esquema es **exclusivo** de Restaurante y se llama `restaurante_portafolio`. No reutiliza ni modifica ninguna de las bases de datos de otros proyectos de XAMPP. Requiere MySQL 8 con motor InnoDB.

## Instalación local con XAMPP

1. Inicia el servidor MySQL de XAMPP.
2. Abre `http://localhost/phpmyadmin` e inicia sesión con tu usuario administrador.
3. Ve a **SQL** e importa/ejecuta `database/01_esquema.sql`. El script crea la base y las tablas si no existen; **no borra registros**.
4. En **Cuentas de usuarios**, crea una cuenta MySQL específica (ejemplo: `restaurante_app` con Host `127.0.0.1`) y asigna una contraseña nueva y única. Da permisos `SELECT`, `INSERT` y `UPDATE` sobre `restaurante_portafolio`. Para ejecutar la demostración no hace falta `DROP`, `CREATE USER` ni acceso a otras bases.
5. Define las variables de entorno en **la misma ventana PowerShell** desde donde ejecutarás Maven.

```powershell
$env:RESTAURANTE_DB_URL = "jdbc:mysql://127.0.0.1:3306/restaurante_portafolio?connectionTimeZone=UTC"
$env:RESTAURANTE_DB_USER = "restaurante_app"
$secure = Read-Host "Contraseña MySQL de restaurante_app" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:RESTAURANTE_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
```

La contraseña se introduce sin aparecer en la consola, no se guarda en el repositorio y solo permanece como variable del proceso y sus procesos hijos mientras dure esta sesión. No compartas el valor ni capturas que lo contengan.

## Demostración persistente

```powershell
mvn -f Restaurante/pom.xml clean verify
mvn -f Restaurante/pom.xml exec:java -Dexec.mainClass=com.demo.restaurante.RestauranteMysqlDemo -Dexec.args=--demo
```

La demostración crea platillos y un empleado de ejemplo si las tablas respectivas están vacías y registra un nuevo pedido. También lee datos guardados, muestra su estado e inicia el primer pendiente. Ejecutarla de nuevo agrega otro pedido; no es un proceso de carga idempotente de pedidos.

La demostración en memoria `com.demo.restaurante.Restaurante` no necesita MySQL.

## Tablas

| Tabla | Datos y restricciones |
| --- | --- |
| `platillos` | Catálogo, costo, precio, activo, validaciones monetarias |
| `empleados` | Nombre y puesto |
| `pedidos` | Cliente, estado, ID autoincremental, fecha |
| `pedido_detalles` | Platillo, nombre y precio históricos, cantidad; claves foráneas |

Los precios históricos se guardan en `pedido_detalles`; los totales se calculan en Java a partir de estas instantáneas, para evitar valores redundantes.

## Pruebas MySQL en CI

GitHub Actions crea un contenedor MySQL 8.4, una base **separada** llamada `restaurante_ci` y credenciales efímeras de prueba. JUnit aplica las tablas y comprueba transacciones, cambios de estado, FIFO, reconstrucción y precios históricos. La prueba se niega a limpiar una base que no se llame exactamente `restaurante_ci`.

## Alcance y pendientes

- Este esquema es inicial: futuras versiones exigirán migraciones controladas.
- La aplicación no expone una API ni operaciones concurrentes generales, aunque los cambios de estado usan bloqueos de fila.
- Los pedidos deben generarse a partir del catálogo vigente; el repositorio compara ID, nombre y precio en la base antes de registrar.
- No permite eliminar platillos referenciados por pedidos para conservar integridad.
- Sin usuarios autenticados, pagos, facturación o datos de clientes identificables.

## Migración de fase 7 (obligatoria para crear platillos desde la web)

Antes de importar, **respalda la base `restaurante_portafolio` y detén Spring Boot**: esta migración requiere retirar y recrear temporalmente una clave foránea. Si alguna instrucción falla, no continúes ejecutando cambios sin revisar el estado de las restricciones.\n\nDesde phpMyAdmin (con una cuenta administradora), selecciona la base `restaurante_portafolio` e importa **`database/03_migracion_catalogo.sql`**. Esta migración convierte la columna primaria `platillos.id` en **AUTO_INCREMENT**, conservando los IDs y los datos de todos los platillos existentes. No modifica los pedidos ni otras bases. Ejecuta primero las fases 1 y 2 si aún no las tienes.

Después de esta migración, `restaurante_app` mantiene únicamente `SELECT, INSERT, UPDATE`: las acciones diarias del catálogo NO requieren `ALTER` ni `DELETE`. No hay borrado físico de platillos, para conservar la integridad de las claves foráneas.

**Importante:** la aplicación aún no dispone de autenticación/autorización ni protección CSRF. Se mantiene enlazada a `127.0.0.1` para pruebas locales. No publiques los formularios de administración en Internet.

## Fase 8 — cuentas de aplicación

Desde la fase 8, importar también `database/04_usuarios_seguridad.sql` con el servidor detenido y crear usuarios de acceso con BCrypt mediante el bootstrap documentado en [docs/SEGURIDAD.md](../docs/SEGURIDAD.md). El usuario de MySQL `restaurante_app` sigue siendo una cuenta técnica, **no es un usuario de inicio de sesión web**.

## Fase 9C — Auditoría
Con el servidor detenido y luego de respaldar `restaurante_portafolio`, importa `database/05_auditoria_operaciones.sql` desde phpMyAdmin. Agrega solamente la tabla `auditoria_operaciones`; no modifica las tablas anteriores. El usuario `restaurante_app` necesita `SELECT` e `INSERT` sobre ella. Véase [docs/SEGURIDAD.md](../docs/SEGURIDAD.md) para limitaciones y pruebas.
