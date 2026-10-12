package com.inventrack.inventrack.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * Carga db/inventrack.sql en una base vacía. Está apagado por defecto: solo actúa si
 * DB_INIT=true y la tabla "categorias" todavía no existe, así que es seguro dejarlo
 * activado en cada arranque (no duplica datos).
 * Si además DB_RESET=true, antes de cargar borra el esquema public (todas las tablas y
 * datos); solo debe usarse una vez y volver a dejarlo en false.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;

    @Value("${mobile.enabled:true}")
    private boolean mobileEnabled;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // El almacén tiene su inicialización idempotente. Nunca ejecuta DB_RESET.
        if (mobileEnabled) {
            log.info("Modo almacén: se usa el inicializador móvil; DB_INIT y DB_RESET se ignoran.");
            return;
        }
        if (!"true".equalsIgnoreCase(System.getenv("DB_INIT"))) {
            return;
        }
        String ruta = System.getenv().getOrDefault("DB_INIT_SCRIPT", "db/inventrack.sql");
        try (Connection con = dataSource.getConnection()) {
            if ("true".equalsIgnoreCase(System.getenv("DB_RESET"))) {
                log.warn("DB_RESET: se borra el esquema public y se vuelve a cargar la base.");
                try (var st = con.createStatement()) {
                    st.execute("drop schema public cascade");
                    st.execute("create schema public");
                }
            }
            try (var st = con.createStatement();
                 var rs = st.executeQuery("select to_regclass('public.categorias')")) {
                rs.next();
                if (rs.getString(1) != null) {
                    log.info("DB_INIT: la base ya tiene tablas, no se carga nada.");
                    return;
                }
            }
            Path script = Path.of(ruta);
            if (!Files.exists(script)) {
                throw new IllegalStateException("DB_INIT: no existe el script " + script.toAbsolutePath());
            }
            log.info("DB_INIT: base vacía, cargando {}", script);
            ScriptUtils.executeSqlScript(con, new PathResource(script));
            log.info("DB_INIT: script cargado correctamente.");
        }
    }
}
