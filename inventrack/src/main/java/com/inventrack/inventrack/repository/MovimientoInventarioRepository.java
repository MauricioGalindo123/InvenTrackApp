package com.inventrack.inventrack.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventrack.inventrack.entity.MovimientoInventario;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Integer> {

    List<MovimientoInventario>
        findBySucursal_IdAndFechaBetweenOrderByFechaAsc(
            Integer idSucursal,
            LocalDateTime desde,
            LocalDateTime hasta
        );

    List<MovimientoInventario>
        findByProducto_IdAndSucursal_IdOrderByFechaAsc(
            Integer idProducto,
            Integer idSucursal
        );
}