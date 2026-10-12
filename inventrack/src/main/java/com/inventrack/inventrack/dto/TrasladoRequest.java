package com.inventrack.inventrack.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TrasladoRequest(
        @NotNull(message = "idSucursalOrigen es obligatorio") Integer idSucursalOrigen,
        @NotNull(message = "idSucursalDestino es obligatorio") Integer idSucursalDestino,
        @NotNull(message = "idUsuario es obligatorio") Integer idUsuario,
        String observacion,
        @NotEmpty(message = "detalle no puede estar vacío") List<@Valid Item> detalle) {

    public record Item(
            @NotNull(message = "idProducto es obligatorio") Integer idProducto,
            @NotNull @Positive(message = "cantidad debe ser mayor a 0") Integer cantidad) {
    }
}
