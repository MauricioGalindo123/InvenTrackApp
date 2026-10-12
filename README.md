# InvenTrackApp · Almacén M&M

Aplicación web de gestión de almacén, independiente para M&M: Android mediante la APK e iPhone mediante la web instalable. El repositorio elegido para esta versión es [MauricioGalindo123/InvenTrackApp](https://github.com/MauricioGalindo123/InvenTrackApp).

- Cuentas del negocio y sesión guardada durante 30 días, renovable al abrir la aplicación.
- Catálogo de Kayfácil: **4.566 productos con cantidad inicial 0**, pendientes de conteo.
- Tres sitios: **Almacén general · 4.º piso**, **Altillo Beauty** y **Altillo M&M**, con cantidades, conteos, ubicaciones e historial independientes.
- Selector de almacén visible en el teléfono, con la última elección guardada por usuario en ese dispositivo.
- Conteos, ingresos y salidas de varios productos con destino y usuario identificado.
- Importación Excel de códigos nuevos en un catálogo compartido. Los existentes se omiten y los nuevos empiezan en cero en cada sitio; el stock registrado se conserva.
- Exportación Excel del sitio seleccionado, identificado dentro del archivo.
- Un aviso agrupado por teléfono para los tres sitios. Al tocarlo, **Por reponer** los muestra separados por almacén.

El stock existente pertenece al almacén general y se conserva al actualizar. Los dos altillos empiezan pendientes de conteo. El código, nombre, precio y mínimo de cada producto son comunes al catálogo; las unidades y la ubicación se registran por sitio.

La API autenticada ofrece `GET /api/mobile/warehouses`. Los endpoints de existencias, movimientos y exportaciones reciben el ID elegido mediante `X-Almacen-Id`. Omitirlo mantiene el almacén general para los clientes anteriores. La migración añade columnas y existencias faltantes; conserva los IDs, usuarios, sesiones e historial anteriores.

## Publicar

El proyecto está publicado en **MauricioGalindo123/InvenTrackApp**, rama `main`. La guía [PUBLICAR_RENDER.md](docs/PUBLICAR_RENDER.md) indica cómo activar el Blueprint preparado y obtener el enlace de la APK.

[Abrir InvenTrack](https://inventrack-almacen.onrender.com). Para actualizar este servicio existente, publica los cambios en `main` y comprueba el despliegue. Si la integración de Git no lo inicia, usa **Manual Deploy → Deploy latest commit**; no hace falta crear otro servicio o base.

[Publicar la versión de prueba en Render](https://render.com/deploy?repo=https%3A%2F%2Fgithub.com%2FMauricioGalindo123%2FInvenTrackApp)

El Blueprint conecta el servidor con la base `inventrack-almacen-db`, ya creada en **My Workspace**, región Virginia. No crea otra base ni incluye sus contraseñas. La base de prueba vence el **11 de noviembre de 2026**; antes de cargar cantidades reales, configura almacenamiento permanente.

El backend usa Java 25, Spring Boot, Maven y PostgreSQL. El Dockerfile compila el servidor y sirve también la interfaz móvil. Las credenciales se configuran en Render y no se incluyen en el código.

## Ejecutar en tu computadora

Desde `inventrack/`, define `MOBILE_SETUP_TOKEN` con una clave privada para activar tu primera cuenta y ejecuta:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

En Windows puedes usar `mvnw.cmd`. El perfil `local` usa H2 dentro de `inventrack/data/`; en Render se usa PostgreSQL. Abre `http://localhost:8080` y crea tu cuenta con la clave elegida.

## Verificación y avisos

```sh
cd inventrack
./mvnw test
```

Se conserva la compatibilidad del backend usado como base, junto con las pruebas del almacén. El modo móvil está activo por defecto y bloquea el panel y los endpoints del curso. No reinicia las cantidades al arrancar.

Consulta [AVISOS.md](docs/AVISOS.md) para configurar Firebase o VAPID. La entrega de notificaciones necesita probarse en teléfonos reales con esas credenciales.

Base de código: [InvenTrackApp del curso](https://github.com/DAM2323/InvenTrackApp). El historial de esta versión conserva el commit inicial del repositorio independiente de MauricioGalindo123.
