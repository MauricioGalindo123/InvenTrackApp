package com.inventrack.inventrack.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.DetalleTraslado;

public interface DetalleTrasladoRepository extends JpaRepository<DetalleTraslado, Integer> {
    List<DetalleTraslado> findByTraslado_Id(Integer idTraslado);
}
