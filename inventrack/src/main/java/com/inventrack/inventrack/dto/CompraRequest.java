package com.inventrack.inventrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CompraRequest(
        @NotNull(message = "idProveedor es obligatorio")
        Integer idProveedor,

        @NotNull(message = "idSucursal es obligatorio")
        Integer idSucursal,

        @NotNull(message = "idUsuario es obligatorio")
        Integer idUsuario,

        String observacion,

        @NotEmpty(message = "El detalle de la compra no puede estar vacío")
        @Valid
        List<Item> detalle
) {
    public record Item(
            @NotNull(message = "idProducto es obligatorio")
            Integer idProducto,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a 0")
            Integer cantidad,

            @NotNull(message = "El precio unitario es obligatorio")
            @Positive(message = "El precio unitario debe ser mayor a 0")
            BigDecimal precioUnitario
    ) {}
}