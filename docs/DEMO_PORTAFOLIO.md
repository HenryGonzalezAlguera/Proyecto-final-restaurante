# Demo guiada — Restaurante (aprox. 3 a 5 minutos)

Esta guía permite enseñar capacidades técnicas **sin dar acceso público al servidor local**. Realiza las pruebas con datos ficticios y una cuenta de demostración, nunca con claves personales.

## Preparación

- [ ] Confirmar que `mvn -f pom.xml clean verify` termina correctamente.
- [ ] MySQL 8 disponible y esquema `restaurante_portafolio` con migraciones 01, 03, 04 y 05; la carga 02 es opcional.
- [ ] Iniciar Spring Boot en `127.0.0.1:8085`.
- [ ] Verificar que puedes iniciar sesión como administrador; el repositorio **no contiene** usuario/contraseña públicos.
- [ ] Usar solo IDs, nombres y cuentas ficticias. Evitar nombres completos, números reales, correos privados o contraseñas en capturas.
- [ ] Mantener abierta la página de [GitHub Actions](https://github.com/HenryGonzalezAlguera/Proyecto-final-restaurante/actions/workflows/java-maven.yml) para evidenciar CI.

## Guion para una demostración técnica

| Momento | Acción visible | Competencia demostrada |
| --- | --- | --- |
| 0:00–0:30 | Mostrar login y entrar como `ADMIN` | Spring Security, sesiones y BCrypt |
| 0:30–1:10 | Abrir Dashboard y explicar que son métricas operativas, **no cobros** | Cálculo de indicadores desde los pedidos |
| 1:10–2:00 | Crear pedido de prueba y cambiar su estado válido | Validaciones, estados y persistencia MySQL |
| 2:00–2:30 | Abrir Historial, buscar un pedido y consultar sus detalles | Lectura de datos persistidos |
| 2:30–3:10 | Editar y desactivar un platillo (sin borrar pedidos anteriores) | CRUD controlado e integridad histórica |
| 3:10–3:45 | Mostrar roles y Auditoría para identificar actor/acción/fecha | Autorización y trazabilidad operativa |
| 3:45–4:30 | Mostrar código del repositorio y GitHub Actions | Arquitectura modular, JDBC, JUnit, MockMvc y CI |

### Texto breve para explicar la arquitectura

> Es una aplicación con dos módulos Maven. El primero encapsula la lógica Java y el repositorio JDBC; el segundo expone las vistas MVC y una API REST. Las reglas del ciclo de vida de pedidos se mantienen en el dominio. MySQL conserva los detalles históricos y Spring Security protege el acceso. GitHub Actions ejecuta pruebas unitarias, HTTP e integración con MySQL.

## Verificaciones rápidas

| Escenario | Resultado esperado |
| --- | --- |
| Sesión anónima visita `/pedidos` | Redirección a login |
| `EMPLEADO` entra a `/usuarios`, `/catalogo` o `/auditoria` | Acceso denegado |
| Usuario crea pedido válido | Nuevo ID y estado `PENDIENTE` |
| Se envía el siguiente pendiente | Pasa a `EN_PREPARACION` conforme al orden |
| Se actualiza el precio de un platillo | Los pedidos históricos conservan su precio |
| Se crea un pedido o modifica un empleado | Evento correspondiente en Auditoría si el registro fue exitoso |
| POST sin token CSRF | HTTP 403 |

## Nota sobre la bitácora y límites

Auditoría se escribe después de la operación, con una transacción diferente. No garantiza que toda operación tenga un evento si falla MySQL entre ambos pasos. El bloqueo temporal del login es local y está guardado en memoria. El prototipo tampoco procesa pagos ni reservas.

## Material para reclutamiento

- URL del repositorio: https://github.com/HenryGonzalezAlguera/Proyecto-final-restaurante
- Introducción, tecnologías y pasos de instalación: [README principal](../README.md)
- Diseño técnico: [ARQUITECTURA.md](ARQUITECTURA.md)
- Evidencia visual segura: [CAPTURAS.md](CAPTURAS.md)
- Borrador listo para adaptar a LinkedIn: [PUBLICACION_LINKEDIN.md](PUBLICACION_LINKEDIN.md)

**No afirmar** que la aplicación está desplegada o que está disponible como SaaS. Para verla funcionando en otro equipo, es necesario ejecutar un entorno propio y configurar MySQL.
