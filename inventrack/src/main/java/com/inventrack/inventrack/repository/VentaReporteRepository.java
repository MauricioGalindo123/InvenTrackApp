package com.inventrack.inventrack.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventrack.inventrack.entity.Venta;

public interface VentaReporteRepository
        extends JpaRepository<Venta, Integer> {

    List<Venta> findByEstadoAndFechaVentaBetween(
        String estado,
        LocalDateTime desde,
        LocalDateTime hasta
    );
}