package com.inventrack.inventrack.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Traslado;

public interface TrasladoRepository extends JpaRepository<Traslado, Integer> {
    List<Traslado> findBySucursalOrigen_IdOrSucursalDestino_Id(Integer idOrigen, Integer idDestino);
}
