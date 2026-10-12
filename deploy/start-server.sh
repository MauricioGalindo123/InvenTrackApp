#!/bin/sh
set -eu

# Render entrega una URL PostgreSQL; Spring necesita JDBC y credenciales separadas.
if [ -z "${DB_URL:-}" ]; then
  case "${RENDER_DATABASE_URL:-}" in
    postgresql://*|postgres://*) ;;
    *) echo 'Configura DB_URL o RENDER_DATABASE_URL para conectar PostgreSQL.' >&2; exit 1 ;;
  esac
  database_location=${RENDER_DATABASE_URL#*://}
  database_location=${database_location##*@}
  case "$database_location" in
    ''|/*) echo 'La conexión PostgreSQL no contiene un servidor válido.' >&2; exit 1 ;;
  esac
  DB_URL="jdbc:postgresql://$database_location"
  case "$DB_URL" in
    *\?sslmode=*|*\&sslmode=*) ;;
    *\?*) DB_URL="$DB_URL&sslmode=require" ;;
    *) DB_URL="$DB_URL?sslmode=require" ;;
  esac
  export DB_URL
fi

# Conserva el mismo valor predeterminado que application.properties.
DB_USER=${DB_USER:-postgres}
export DB_USER
: "${DB_PASSWORD:?Configura DB_PASSWORD con la contraseña de PostgreSQL.}"
case "$DB_URL" in
  jdbc:postgresql://*) ;;
  *) echo 'DB_URL debe ser una URL JDBC de PostgreSQL.' >&2; exit 1 ;;
esac

# Deja memoria para el servidor web, las librerías y las importaciones de Excel.
exec java -XX:MaxRAMPercentage=50 -XX:ActiveProcessorCount=2 -jar /app/app.jar
