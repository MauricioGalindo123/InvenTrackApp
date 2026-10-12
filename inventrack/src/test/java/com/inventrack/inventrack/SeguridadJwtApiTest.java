package com.inventrack.inventrack;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.inventrack.inventrack.config.DatabaseInitializer;
import com.jayway.jsonpath.JsonPath;

/** Comprueba la seguridad con token JWT sobre una base H2 temporal. */
@SpringBootTest(properties = {
        "mobile.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:seguridad;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../db/inventrack.sql",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class SeguridadJwtApiTest {

    private static final String LOGIN = "{\"correo\":\"admin@inventrack.pe\",\"contrasena\":\"ClaveSegura2026*\"}";

    @Autowired
    private MockMvc mvc;

    // La prueba carga su propia base temporal, independientemente de DB_INIT/DB_RESET.
    @MockitoBean
    private DatabaseInitializer databaseInitializer;

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/sucursal/listar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token requerido o inválido"));
    }

    @Test
    void conTokenInvalidoResponde401() throws Exception {
        mvc.perform(get("/sucursal/listar").header("Authorization", "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEsPublicoYDevuelveElToken() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").exists())
                .andExpect(jsonPath("$.bearer").value("Bearer"))
                .andExpect(jsonPath("$.nombreCompleto").exists())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConClaveIncorrectaResponde401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"admin@inventrack.pe\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoSeAccedeALosEndpoints() throws Exception {
        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(cuerpo, "$.token");
        mvc.perform(get("/sucursal/listar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/inventario/listar").param("idSucursal", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void laPaginaDeInicioEsPublica() throws Exception {
        mvc.perform(get("/academico.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("InvenTrack API")));
    }

    @Test
    void getEnLoginRespondeJsonConMetodoNoPermitido() throws Exception {
        mvc.perform(get("/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensaje").value("Método GET no permitido en esta ruta"));
    }
}
