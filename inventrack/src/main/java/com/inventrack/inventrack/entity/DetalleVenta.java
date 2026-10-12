package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_ventas")
@Getter
@Setter
@NoArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iddetalleventa")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idventa", nullable = false)
    private Venta venta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idproducto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "preciounitario")
    private BigDecimal precioUnitario;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "subtotal")
    private BigDecimal subtotal;
}
