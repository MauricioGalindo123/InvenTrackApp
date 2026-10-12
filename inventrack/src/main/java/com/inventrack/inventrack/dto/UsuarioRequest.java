package com.inventrack.inventrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        Integer idUsuario,
        @NotNull(message = "idSucursal es obligatorio") Integer idSucursal,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio") String correo,
        String contrasena,
        @NotBlank(message = "El rol es obligatorio") String rol) {
}