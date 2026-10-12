ALTER TABLE inventario ADD COLUMN IF NOT EXISTS conteoreal BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE producto ADD COLUMN IF NOT EXISTS imagenurl VARCHAR(1000);
ALTER TABLE movimiento_inventario ADD COLUMN IF NOT EXISTS idoperacion VARCHAR(100);
CREATE TABLE IF NOT EXISTS mobile_sesion (
 tokenhash VARCHAR(64) PRIMARY KEY, idusuario INT NOT NULL REFERENCES usuario(idusuario),
 csrf VARCHAR(64) NOT NULL, expira TIMESTAMP NOT NULL, creado TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS mobile_operacion (
 id VARCHAR(100) PRIMARY KEY, idusuario INT REFERENCES usuario(idusuario),
 idsucursal INT NOT NULL REFERENCES sucursales(idsucursal), tipo VARCHAR(30) NOT NULL,
 destino VARCHAR(120), observacion VARCHAR(500), fecha TIMESTAMP NOT NULL,
 huella VARCHAR(64) NOT NULL, resultado TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS mobile_dispositivo (
 id VARCHAR(64) PRIMARY KEY, idusuario INT NOT NULL REFERENCES usuario(idusuario),
 plataforma VARCHAR(10) NOT NULL, endpoint VARCHAR(2000) NOT NULL,
 p256dh VARCHAR(250), auth VARCHAR(100), notificados TEXT NOT NULL DEFAULT '[]',
 ultimoenvio TIMESTAMP, activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_mobile_mov_fecha ON movimiento_inventario(idsucursal, fecha);
CREATE INDEX IF NOT EXISTS idx_mobile_sesion_usuario ON mobile_sesion(idusuario);
CREATE INDEX IF NOT EXISTS idx_mobile_dispositivo_usuario ON mobile_dispositivo(idusuario);
