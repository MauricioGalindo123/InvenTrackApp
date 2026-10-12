package com.inventrack.inventrack.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.DetalleVenta;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {

    List<DetalleVenta> findByVenta_Id(Integer idVenta);
}