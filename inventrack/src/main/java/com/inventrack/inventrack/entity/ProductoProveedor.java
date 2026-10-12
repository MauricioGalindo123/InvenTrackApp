package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class ProductoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idproductoproveedor")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idproducto", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idproveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "codigoproveedor")
    private String codigoProveedor;

    @Column(name = "preciocompra")
    private BigDecimal precioCompra;

    @Column(name = "activo")
    private Boolean activo;
}
