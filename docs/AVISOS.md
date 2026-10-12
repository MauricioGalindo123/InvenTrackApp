# Avisos de reposición

La aplicación muestra la lista de productos por reponer sin servicios externos. Para recibir una notificación con la aplicación cerrada debes activar el servicio push del servidor y permitir los avisos en cada teléfono.

El servidor revisa el stock cada 30 segundos. Envía un solo resumen por dispositivo cuando aparecen nuevos productos con bajo stock y limita los envíos a uno por minuto. No repite el mismo aviso en cada revisión ni al renovar el token del teléfono. Android utiliza un único identificador de notificación y la web una única etiqueta `stock-bajo`; las actualizaciones reemplazan ese aviso. Tocarlo abre **Por reponer**. La lista consultada al abrir muestra las cantidades actuales.

Los ceros que todavía están pendientes de conteo no se notifican. Después de reponer un producto, si vuelve a quedar bajo su mínimo se podrá incluir en un nuevo resumen. La entrega depende de internet, los permisos del teléfono y el servicio de notificaciones.

## Android con Firebase

1. Crea un proyecto de Firebase y registra una aplicación Android con el paquete **pe.inventrack.almacen**. Activa Firebase Cloud Messaging.
2. Obtén la configuración pública de esa aplicación en Firebase: API key, App ID, Project ID y Sender ID. El App ID debe corresponder a Android.
3. Genera una cuenta de servicio del mismo proyecto con permisos para enviar mensajes FCM. Guarda su JSON como variable privada del servidor, fuera del repositorio.
4. Configura las cinco variables del backend y reinicia el servicio:

| Variable | Contenido |
| --- | --- |
| `MOBILE_FIREBASE_SERVICE_ACCOUNT` | JSON completo de la cuenta de servicio. Es privado. |
| `MOBILE_FIREBASE_API_KEY` | API key de la configuración Android. |
| `MOBILE_FIREBASE_APP_ID` | App ID Android, con formato `1:…:android:…`. |
| `MOBILE_FIREBASE_PROJECT_ID` | Project ID. |
| `MOBILE_FIREBASE_SENDER_ID` | Número de proyecto / Sender ID. |

La APK obtiene esta configuración pública del backend y registra su token al pulsar **Mi cuenta → Activar en este teléfono**. No necesita incluir un JSON privado dentro de la APK. Al actualizar el token FCM lo vuelve a registrar con la sesión vigente; también lo comprueba al abrir la aplicación. El servidor manda mensajes de datos agrupados y Android crea una única notificación que abre la pantalla de reposición.

## Web e iPhone con VAPID

1. Con Node.js instalado, ejecuta `node tools/generar-vapid.mjs` una sola vez en tu computadora.
2. Guarda `MOBILE_VAPID_PUBLIC` y `MOBILE_VAPID_PRIVATE` en las variables del servidor. Conserva el mismo par al reiniciar o desplegar una actualización.
3. Configura `MOBILE_VAPID_SUBJECT` con un correo de contacto, por ejemplo `mailto:tu-correo@example.com`.
4. Publica el backend con HTTPS. En iPhone con iOS 16.4 o posterior abre Safari, agrega la aplicación a Inicio, ábrela desde ese icono e inicia sesión.
5. Pulsa **Mi cuenta → Activar en este teléfono** y acepta el permiso.

El service worker recibe el aviso aun sin mantener la pantalla abierta. Las suscripciones pertenecen al usuario que las activa. Cerrar sesión desactiva los avisos de ese dispositivo; no cierra las sesiones de sus otros teléfonos.

## Prueba real pendiente

Esta entrega verifica la agrupación y los reintentos con un transporte de prueba. No se enviaron notificaciones reales porque no se proporcionaron credenciales de Firebase o VAPID. Tras publicar el servicio propio de este almacén en Render y configurar los servicios, prueba Android e iPhone por separado: confirma el conteo de dos productos con cantidades bajo su mínimo, espera la revisión y comprueba que aparece un solo resumen, que no se repite continuamente y que al tocarlo abre la lista correcta.
