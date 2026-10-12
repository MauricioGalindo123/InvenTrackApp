package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idproducto")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idcategorias", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "idmarca")
    private Marca marca;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precioventa")
    private BigDecimal precioVenta;

    @Column(name = "preciocosto")
    private BigDecimal precioCosto;

    @Column(name = "stockminimo")
    private Integer stockMinimo;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "fechacreacion")
    private LocalDateTime fechaCreacion;
}
