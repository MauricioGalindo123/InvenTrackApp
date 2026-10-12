package com.inventrack.inventrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Datos de PUT /inventario/actualizar-ubicacion, según HU-INV-004. */
@Getter
@Setter
public class UbicacionRequest {

    @NotNull(message = "idInventario es obligatorio")
    @Positive(message = "idInventario debe ser mayor que cero")
    private Integer idInventario;

    @NotBlank(message = "ubicacion es obligatoria")
    @Size(max = 100, message = "ubicacion no puede superar los 100 caracteres")
    private String ubicacion;
}
