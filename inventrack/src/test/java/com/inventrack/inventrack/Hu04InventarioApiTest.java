package com.inventrack.inventrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.inventrack.inventrack.config.DatabaseInitializer;
import com.inventrack.inventrack.repository.InventarioRepository;

/** Ejecuta la API y los repositorios reales sobre una base H2 temporal. */
@SpringBootTest(properties = {
        "mobile.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:hu04;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../db/inventrack.sql",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class Hu04InventarioApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InventarioRepository inventarioRepo;

    // La prueba carga su propia base temporal, independientemente de DB_INIT/DB_RESET.
    @MockitoBean
    private DatabaseInitializer databaseInitializer;

    @Test
    void tc001ListaLosSieteProductosDeLaSucursal() throws Exception {
        mvc.perform(get("/inventario/listar").param("idSucursal", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[*].sucursal.id").value(containsInAnyOrder(1, 1, 1, 1, 1, 1, 1)))
                .andExpect(jsonPath("$[?(@.id == 1)].cantidadDisponible").value(containsInAnyOrder(8)))
                .andExpect(jsonPath("$[?(@.id == 1)].ubicacion").value(containsInAnyOrder("Pasillo A-1")))
                .andExpect(jsonPath("$[?(@.id == 1)].producto.stockMinimo").value(containsInAnyOrder(5)));
    }

    @Test
    void tc002BuscaElProductoEnLaSucursalIndicada() throws Exception {
        mvc.perform(get("/inventario/buscar").param("idSucursal", "1").param("idProducto", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sucursal.id").value(1))
                .andExpect(jsonPath("$.producto.id").value(1))
                .andExpect(jsonPath("$.cantidadDisponible").value(8));
    }

    @Test
    void tc003ReportaLosCuatroProductosBajoMinimo() throws Exception {
        mvc.perform(get("/reporte/stock-bajo").param("idSucursal", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].idProducto").value(containsInAnyOrder(3, 4, 6, 8)))
                .andExpect(jsonPath("$[?(@.idProducto == 3)].disponible").value(containsInAnyOrder(1)))
                .andExpect(jsonPath("$[?(@.idProducto == 3)].minimo").value(containsInAnyOrder(5)))
                .andExpect(jsonPath("$[?(@.idProducto == 6)].disponible").value(containsInAnyOrder(5)))
                .andExpect(jsonPath("$[?(@.idProducto == 6)].minimo").value(containsInAnyOrder(8)))
                .andExpect(jsonPath("$[?(@.idProducto == 4)].disponible").value(containsInAnyOrder(20)))
                .andExpect(jsonPath("$[?(@.idProducto == 4)].minimo").value(containsInAnyOrder(40)))
                .andExpect(jsonPath("$[?(@.idProducto == 8)].disponible").value(containsInAnyOrder(40)))
                .andExpect(jsonPath("$[?(@.idProducto == 8)].minimo").value(containsInAnyOrder(60)));
    }

    @Test
    void tc004SinAlertasDevuelveUnaListaVacia() throws Exception {
        mvc.perform(get("/reporte/stock-bajo").param("idSucursal", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void tc005ProductoSinInventarioDevuelve404() throws Exception {
        mvc.perform(get("/inventario/buscar").param("idSucursal", "1").param("idProducto", "14"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No hay inventario para ese producto"));
    }

    @Test
    void tc006ActualizaLaUbicacionSinCambiarElStock() throws Exception {
        LocalDateTime inicio = LocalDateTime.now();
        mvc.perform(put("/inventario/actualizar-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idInventario": 1, "ubicacion": "Pasillo Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ubicacion").value("Pasillo Z"))
                .andExpect(jsonPath("$.cantidadDisponible").value(8));

        var actualizado = inventarioRepo.findById(1).orElseThrow();
        assertThat(actualizado.getUbicacion()).isEqualTo("Pasillo Z");
        assertThat(actualizado.getCantidadDisponible()).isEqualTo(8);
        assertThat(actualizado.getProducto().getId()).isEqualTo(1);
        assertThat(actualizado.getSucursal().getId()).isEqualTo(1);
        assertThat(actualizado.getActualizacion()).isAfterOrEqualTo(inicio);
    }

    @Test
    void incluyeElStockIgualAlMinimoEnLasAlertas() throws Exception {
        mvc.perform(get("/reporte/stock-bajo").param("idSucursal", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idProducto == 5)].disponible").value(containsInAnyOrder(20)))
                .andExpect(jsonPath("$[?(@.idProducto == 5)].minimo").value(containsInAnyOrder(20)));
    }

    @Test
    void laBusquedaNoMezclaElStockDeOtrasSucursales() throws Exception {
        mvc.perform(get("/inventario/buscar").param("idSucursal", "4").param("idProducto", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucursal.id").value(4))
                .andExpect(jsonPath("$.cantidadDisponible").value(6));
    }

    @Test
    void ubicacionDeInventarioInexistenteDevuelve404() throws Exception {
        mvc.perform(put("/inventario/actualizar-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idInventario\": 9999, \"ubicacion\": \"Pasillo Z\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Inventario no encontrado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"ubicacion\":\"Pasillo Z\"}",
            "{\"idInventario\":1}",
            "{\"idInventario\":1,\"ubicacion\":\"   \"}",
            "{\"idInventario\":0,\"ubicacion\":\"Pasillo Z\"}"
    })
    void datosInvalidosNoModificanLaUbicacion(String json) throws Exception {
        mvc.perform(put("/inventario/actualizar-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertThat(inventarioRepo.findById(1).orElseThrow().getUbicacion()).isEqualTo("Pasillo A-1");
    }

    @Test
    void noPermiteUbicacionesMasLargasQueLaColumnaDeLaBase() throws Exception {
        mvc.perform(put("/inventario/actualizar-ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idInventario\":1,\"ubicacion\":\"" + "Z".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parametroObligatorioAusenteDevuelve400() throws Exception {
        mvc.perform(get("/inventario/listar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parametroNoNumericoDevuelve400() throws Exception {
        mvc.perform(get("/inventario/buscar").param("idSucursal", "abc").param("idProducto", "1"))
                .andExpect(status().isBadRequest());
    }
}
