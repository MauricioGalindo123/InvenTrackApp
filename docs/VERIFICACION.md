# Verificación de InvenTrack Almacén

Se ejecutó `./mvnw test` con Java 25 dentro de este proyecto independiente: **37 pruebas, 0 fallos, 0 errores y 0 omitidas**.

Las pruebas incluyen:

- Catálogo de 4.566 productos en cero y pendientes de conteo, sin avisos falsos.
- Cuentas propias, sesión persistente, permisos y protección CSRF.
- Importación Excel que conserva cantidades y crea productos nuevos en cero.
- Salida con destino y detalle de productos; reversión completa si falta stock.
- Salidas simultáneas sin stock negativo y reintentos sin doble descuento.
- Notificación agrupada verificada con un transporte de prueba.
- Compatibilidad de las funciones del backend usado como base.

También se comprobó la sintaxis de `app.js` y `sw.js`. Las pruebas de API usaron H2 temporal. El despliegue en PostgreSQL de Render y las notificaciones reales en Android e iPhone requieren la conexión y las credenciales correspondientes.

Esta integración conserva el commit inicial de `MauricioGalindo123/InvenTrackApp` (`97820ae`) y usa ese repositorio como `origin`. Los archivos funcionales y las pruebas coinciden con la versión independiente que aprobó las 37 pruebas; las modificaciones de esta integración ajustan el README y las guías al repositorio elegido. La subida requiere acceso de escritura a GitHub y el despliegue requiere configurar PostgreSQL en Render.
