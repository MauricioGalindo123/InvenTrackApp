package com.inventrack.inventrack.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductoProveedorRequest(
        Integer idProductoProveedor,
        @NotNull(message = "idProducto es obligatorio") Integer idProducto,
        @NotNull(message = "idProveedor es obligatorio") Integer idProveedor,
        String codigoProveedor,
        @PositiveOrZero(message = "precioCompra no puede ser negativo") BigDecimal precioCompra) {
}
