package com.inventrack.inventrack.dto;

import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        Integer idProveedor,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String rfcNit,
        String telefono,
        String correo,
        String direccion) {
}
