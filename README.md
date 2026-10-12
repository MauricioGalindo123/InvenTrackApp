# InvenTrackApp · Almacén M&M

Aplicación web de gestión de almacén, independiente para M&M: Android mediante la APK e iPhone mediante la web instalable. El repositorio elegido para esta versión es [MauricioGalindo123/InvenTrackApp](https://github.com/MauricioGalindo123/InvenTrackApp).

- Cuentas del negocio y sesión guardada durante 30 días, renovable al abrir la aplicación.
- Catálogo de Kayfácil: **4.566 productos con cantidad inicial 0**, pendientes de conteo.
- Conteos, ingresos y salidas de varios productos con destino, usuario e historial.
- Importación y exportación Excel. Importar no reemplaza las cantidades del almacén.
- Lista de productos por reponer y un aviso agrupado por teléfono cuando se configura el servicio push.

## Publicar

Usa **MauricioGalindo123/InvenTrackApp**, con rama `main`. La guía [PUBLICAR_RENDER.md](docs/PUBLICAR_RENDER.md) indica cómo subir el proyecto a ese repositorio y publicar un servicio propio para obtener el enlace de la APK. El ZIP contiene el proyecto completo, dentro de `InvenTrackApp/`.

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
