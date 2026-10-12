# Publicar InvenTrack Almacén en Render

El código completo está en [MauricioGalindo123/InvenTrackApp](https://github.com/MauricioGalindo123/InvenTrackApp), rama `main`. La aplicación conserva el commit inicial del repositorio y usa un servicio propio para el almacén.

## Activar el despliegue preparado

Se creó la base PostgreSQL **inventrack-almacen-db** en **My Workspace**, región **Virginia**. El archivo [render.yaml](../render.yaml) define un servidor Docker gratuito y toma la conexión, el usuario y la contraseña de esa base mediante `fromDatabase`. No contiene credenciales ni crea otra base.

1. Abre [Publicar en Render](https://render.com/deploy?repo=https%3A%2F%2Fgithub.com%2FMauricioGalindo123%2FInvenTrackApp).
2. Usa **My Workspace**. Si Render pide conectar GitHub, permite acceso a **MauricioGalindo123/InvenTrackApp**.
3. Usa la rama **main**, el archivo **render.yaml** y un nombre de Blueprint, por ejemplo **InvenTrack Almacén**.
4. Revisa que se cree únicamente el servidor **inventrack-almacen**, con plan **Free**. La base existente se referencia sin duplicarla. Pulsa **Deploy Blueprint**.
5. Espera a que el despliegue esté **Live**. Render muestra la URL HTTPS del servidor; esa será la dirección para la APK.

El enlace de publicación abre la configuración de Render. No es la dirección de la aplicación. La conexión de Render disponible en ChatGPT permite crear servicios y bases, pero no aplicar Blueprints ni obtener la contraseña de PostgreSQL; por eso la activación del Blueprint requiere este paso en el panel.

## Verificar el servidor

Abre la URL del servicio seguida de `/api/mobile/public-config`. Debe responder JSON con `"version":"1.1.0-render"` y `"catalogoInicial":4566`, sin pedir JWT. La página principal debe mostrar la activación o el inicio de sesión del almacén.

El Blueprint genera una clave privada **MOBILE_SETUP_TOKEN**. Puedes verla en **Environment** del servidor y usarla para crear tu primera cuenta desde la aplicación. No compartas esa clave ni la incluyas en GitHub. El administrador elige su nombre, correo y contraseña; la sesión se conserva en su teléfono.

En la APK pega la URL HTTPS del servidor nuevo, sin añadir `/api` ni `/auth`. En iPhone abre esa misma URL en Safari y usa **Compartir → Agregar a Inicio**.

## Stock y avisos

Los **4.566 productos empiezan en 0 y pendientes de conteo**. La aplicación no toma el stock de Kayfácil como stock del almacén y no marca los productos pendientes de conteo como faltantes. Las importaciones posteriores conservan las cantidades ya registradas. Las salidas indican su destino y no pueden dejar stock negativo.

Antes de cargar cantidades reales, configura una base permanente. La base gratuita creada el 12 de octubre de 2026 vence el **11 de noviembre de 2026**. Render la vuelve inaccesible al vencer y puede eliminarla después del período de gracia de 14 días. No se ha contratado ningún plan de pago.

El servidor Free se suspende tras 15 minutos sin tráfico y vuelve a arrancar al recibir una petición. Para avisos continuos necesita un servidor que permanezca activo. Para configurar una notificación agrupada con la aplicación cerrada, sigue [AVISOS.md](AVISOS.md): faltan las credenciales de Firebase o VAPID y verificar la entrega en teléfonos.

## Configuración alternativa desde el panel

Si creas el servidor manualmente, utiliza **Docker**, rama **main**, raíz del repositorio, `./Dockerfile` y el health check `/api/mobile/public-config`. La base y el servidor deben estar en la misma región.

| Variable | Valor |
| --- | --- |
| `RENDER_DATABASE_URL` | Internal Database URL de PostgreSQL |
| `DB_USER` | Usuario de esa base |
| `DB_PASSWORD` | Contraseña de esa base |
| `MOBILE_ENABLED` | `true` |
| `MOBILE_SETUP_TOKEN` | Clave privada de activación; puede generarse con `node deploy/generar-clave.mjs` |
| `JWT_SECRET` | Clave aleatoria privada de al menos 32 caracteres |
| `DB_INIT` | `false` |
| `DB_RESET` | `false` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | `1` |

También puedes usar `DB_URL` con formato `jdbc:postgresql://servidor:5432/base?sslmode=require` en lugar de `RENDER_DATABASE_URL`, manteniendo usuario y contraseña separados. No uses el perfil `local` en Render. La aplicación crea las tablas y el catálogo inicial de forma idempotente, sin reiniciar cantidades al arrancar.

Referencias oficiales: [Blueprints](https://render.com/docs/infrastructure-as-code), [Referencia del Blueprint](https://render.com/docs/blueprint-spec), [Web Services](https://render.com/docs/web-services) y [Deploy for Free](https://render.com/docs/free).
