package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_traslado")
@Getter
@Setter
@NoArgsConstructor
public class DetalleTraslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iddetalletraslado")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idtraslado", nullable = false)
    private Traslado traslado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idproducto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad")
    private Integer cantidad;
}
