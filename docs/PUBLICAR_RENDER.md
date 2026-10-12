# Publicar InvenTrack Almacén como proyecto independiente

Esta aplicación se publica desde [MauricioGalindo123/InvenTrackApp](https://github.com/MauricioGalindo123/InvenTrackApp), el repositorio independiente que elegiste. Usa un servicio del almacén separado del backend del curso. El enlace anterior `https://inventrack-api-4orj.onrender.com/` pertenece al proyecto anterior; no es el enlace de esta entrega.

El repositorio remoto ya existe: al revisarlo tenía un README y el commit inicial `97820ae`. La incorporación completa del almacén está preparada localmente. El servicio propio del almacén en Render todavía no se ha creado.

## 1. Subir el proyecto al repositorio elegido

No necesitas crear otro repositorio. Usa **https://github.com/MauricioGalindo123/InvenTrackApp.git**. Sube el contenido de la carpeta `InvenTrackApp/` del ZIP: `Dockerfile`, `inventrack/`, `db/`, `deploy/`, `tools/` y los demás archivos deben quedar en la raíz del repositorio. Incluye `inventrack/.mvn/wrapper/maven-wrapper.properties`.

Si usas Git desde tu computadora, clona primero ese repositorio, copia dentro los archivos de esta entrega y sube el cambio. Esto conserva el commit inicial existente:

```sh
git clone https://github.com/MauricioGalindo123/InvenTrackApp.git
cd InvenTrackApp
```

Después de copiar el contenido de `InvenTrackApp/` del ZIP dentro de la carpeta clonada:

```sh
git add .
git commit -m "Agregar almacén móvil M&M"
git push origin main
```

La subida desde ChatGPT necesita conectar GitHub y autorizar la escritura en ese repositorio. El enlace por sí solo permite leer el código público.

## 2. Crear el servicio propio en Render

En [Render](https://dashboard.render.com), usa el espacio **My Workspace** que confirmaste y elige **New → Web Service**. Conecta el repositorio nuevo y configura:

| Campo | Valor |
| --- | --- |
| Repositorio | `MauricioGalindo123/InvenTrackApp` |
| Branch | `main` |
| Name | `inventrack-almacen` |
| Language / Runtime | `Docker` |
| Root Directory | Vacío: la raíz del repositorio |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/api/mobile/public-config` |

Para una demostración puedes seleccionar **Free**. Ese servicio se suspende tras 15 minutos sin tráfico y vuelve a arrancar al recibir una petición; no ofrece funcionamiento continuo para los avisos. Revisa el plan antes de crear el servicio. No se ha contratado ningún recurso de pago.

## 3. Conectar la base del almacén

El servidor necesita una base PostgreSQL persistente para este almacén. Configura su conexión en **Environment**:

| Variable | Valor |
| --- | --- |
| `DB_URL` | URL JDBC de la base del almacén, con formato `jdbc:postgresql://servidor:5432/base?sslmode=require` |
| `DB_USER` | Usuario PostgreSQL de esa base |
| `DB_PASSWORD` | Contraseña PostgreSQL de esa base |
| `MOBILE_ENABLED` | `true` |
| `MOBILE_SETUP_TOKEN` | Clave privada de activación; puedes generarla con `node deploy/generar-clave.mjs` |
| `DB_INIT` | `false` |
| `DB_RESET` | `false` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | `1` |

También puedes usar `RENDER_DATABASE_URL` en lugar de `DB_URL`, manteniendo `DB_USER` y `DB_PASSWORD` separados. La aplicación crea las tablas y su catálogo inicial en una base vacía. No uses el perfil `local` en Render.

En el espacio conectado no se encontró ninguna instancia de Render Postgres durante la revisión. Su creación o una conexión a PostgreSQL externo siguen pendientes. Una base **Free de Render caduca a los 30 días**; no la uses para conservar definitivamente las cantidades de tu negocio.

## 4. Obtener el enlace correcto para la APK

Después de subir el código y configurar PostgreSQL, despliega el servicio nuevo. Espera a que esté **Live** y copia la URL HTTPS que Render asigne. No se ha generado todavía una URL para este proyecto.

Abre esa URL seguida de `/api/mobile/public-config`: debe responder JSON con `"version":"1.1.0-render"` y `"catalogoInicial":4566`, sin pedir JWT. La página principal debe mostrar la activación o el inicio de sesión del almacén.

En la APK pega la **URL del servicio nuevo**, sin añadir `/api` ni `/auth`. Activa tu cuenta una sola vez con `MOBILE_SETUP_TOKEN`. En iPhone abre esa misma URL en Safari y usa **Compartir → Agregar a Inicio**.

## 5. Registrar las cantidades

Los 4.566 productos empiezan en **0 y pendientes de conteo**. Ingresa los conteos reales o registra entradas de mercadería. Las importaciones de Kayfácil ignoran las cantidades del Excel y conservan el stock ya registrado. Las salidas se registran por destino y no pueden dejar stock negativo.

Para activar una notificación agrupada con la aplicación cerrada, sigue [AVISOS.md](AVISOS.md). Hace falta configurar Firebase o VAPID y verificar su entrega en Android e iPhone.

Referencias oficiales: [Web Services](https://render.com/docs/web-services) y [Deploy for Free](https://render.com/docs/free).
