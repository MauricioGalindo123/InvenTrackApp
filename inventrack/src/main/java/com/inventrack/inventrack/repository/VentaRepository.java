package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    boolean existsByCliente_Id(Integer idCliente);
}