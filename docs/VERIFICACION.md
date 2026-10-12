# Verificación de InvenTrack Almacén

Se verificaron **46 pruebas distintas, 0 fallos, 0 errores y 0 omitidas**, con Java 25. Las 22 pruebas de la API móvil se volvieron a ejecutar tras los ajustes finales; las otras 24 verifican la compatibilidad del backend usado como base.

Las pruebas incluyen:

- Catálogo de 4.566 productos en cero y pendientes de conteo, sin avisos falsos.
- Cuentas propias, sesión persistente, permisos y protección CSRF.
- Importación Excel que conserva cantidades y crea productos nuevos en cero.
- Salida con destino y detalle de productos; reversión completa si falta stock.
- Salidas simultáneas sin stock negativo y reintentos sin doble descuento.
- Notificación agrupada verificada con un transporte de prueba.
- Conteos, ingresos, salidas, resúmenes y ubicaciones independientes en los tres almacenes.
- Migración del almacén anterior sin cambiar IDs, cuentas, sesiones, stock ni historial.
- Un mismo código importado una sola vez, con existencias iniciales cero en los tres sitios.
- Excel exportado con las cantidades y el nombre del sitio elegido.
- Reutilización de una operación en otro sitio rechazada, incluso en peticiones simultáneas.
- Un solo aviso que distingue un mismo producto con bajo stock en varios almacenes y conserva los avisos anteriores.
- Compatibilidad de las funciones del backend usado como base.

También se comprobó la sintaxis de `app.js` y `sw.js`. Las pruebas de API usaron H2 temporal. La interfaz se probó en Chromium con anchos de 320, 390 y 1024 píxeles, conectada al servidor Java real con una base local temporal: escritura de cantidades, cambio y persistencia del selector, ajuste desde una alerta de otro sitio, importación de Excel y descarga del archivo del almacén elegido.

El servidor usa PostgreSQL en Render. Firebase y VAPID están configurados, pero la recepción de notificaciones en teléfonos físicos sigue pendiente de comprobar. Las pruebas locales no escriben en la base de producción.

Esta versión conserva el commit inicial de `MauricioGalindo123/InvenTrackApp` (`97820ae`) y usa ese repositorio como `origin`.
