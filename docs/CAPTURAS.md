# Capturas para el portafolio — Pendientes de publicar

El repositorio todavía no incluye fotografías de las pantallas. Para completar la presentación visual, utiliza la carpeta `docs/screenshots/` (ya preparada) y agrega **capturas reales** de una ejecución local, de preferencia con resolución uniforme de unos 1440 × 900 px. No añadas imágenes vacías o generadas que simulen funcionalidad no verificada.

| Archivo sugerido | Pantalla | Qué mostrar |
| --- | --- | --- |
| `01-dashboard.png` | Dashboard | Indicadores operativos y resumen visual |
| `02-pedidos.png` | Pedidos | Formulario, órdenes y estados |
| `03-catalogo.png` | Catálogo | Tarjetas de platillos y controles ADMIN |
| `04-historial.png` | Historial | Filtros y detalles de pedido |
| `05-usuarios.png` | Usuarios | Listado y botones de activar/desactivar (sin campos de contraseña llenos) |
| `06-auditoria.png` | Auditoría | Actor de prueba, acción, referencia y fecha |

## Procedimiento recomendado

1. Ejecuta la aplicación usando **solo datos de demostración** y comprueba que cada pantalla abre sin errores.
2. Oculta barra del navegador, historial de pestañas, notificaciones, barra de tareas y cualquier información ajena a la app; recorta la captura al contenido de la aplicación.
3. No muestres contraseñas, formularios de restablecimiento rellenados, rutas de tu perfil personal, correos, IP externas, cookies, tokens ni datos de clientes reales. En Auditoría usa únicamente una cuenta ficticia.
4. Guarda archivos PNG optimizados y con la misma proporción. Evita imágenes borrosas o con texto diminuto.
5. Sube los PNG a `docs/screenshots/` y añade las referencias en el [README principal](../README.md), **solo cuando los archivos existan realmente**.
6. Antes de compartir en LinkedIn, comprueba que las capturas se ven correctamente en GitHub y en móvil.

## Vista previa de la galería (después de subir las imágenes)

Ejemplo de Markdown que puedes insertar en `README.md` **cuando esos archivos ya estén presentes**:

```markdown
### Capturas
![Dashboard de Restaurante](docs/screenshots/01-dashboard.png)
![Gestión de pedidos](docs/screenshots/02-pedidos.png)
![Catálogo de platillos](docs/screenshots/03-catalogo.png)
![Historial de pedidos](docs/screenshots/04-historial.png)
![Administración de empleados](docs/screenshots/05-usuarios.png)
![Auditoría de operaciones](docs/screenshots/06-auditoria.png)
```

Una galería verificable es preferible a publicar enlaces de imágenes rotas o una demostración que no existe.
