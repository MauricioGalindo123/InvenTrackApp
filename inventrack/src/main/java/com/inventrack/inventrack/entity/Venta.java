package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idventa")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idcliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idsucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idusuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idmetodopago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "fechaventa")
    private LocalDateTime fechaVenta;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "estado")
    private String estado;

    @Column(name = "observacion")
    private String observacion;
}
