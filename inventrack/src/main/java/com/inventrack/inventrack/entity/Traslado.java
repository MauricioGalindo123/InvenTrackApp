package com.inventrack.inventrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "traslados")
@Getter
@Setter
@NoArgsConstructor
public class Traslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idtraslado")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idsucursalorigen", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idsucursaldestino", nullable = false)
    private Sucursal sucursalDestino;

    @ManyToOne(optional = false)
    @JoinColumn(name = "idusuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fechasolicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fechaenvio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecharecepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "estado")
    private String estado;

    @Column(name = "observacion")
    private String observacion;
}
