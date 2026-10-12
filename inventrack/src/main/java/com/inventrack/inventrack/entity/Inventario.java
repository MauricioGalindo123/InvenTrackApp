package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idinventario")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idsucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idproducto", nullable = false)
    private Producto producto;

    @Column(name = "cantidaddisponible")
    private Integer cantidadDisponible;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "actualizacion")
    private LocalDateTime actualizacion;
}
