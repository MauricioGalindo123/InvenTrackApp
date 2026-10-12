package com.inventrack.inventrack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idusuario")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idsucursal", nullable = false)
    private Sucursal sucursal;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "correo")
    private String correo;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "contrasena")
    private String contrasena;

    @Column(name = "rol")
    private String rol;

    @Column(name = "acceso")
    private LocalDateTime acceso;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;
}
