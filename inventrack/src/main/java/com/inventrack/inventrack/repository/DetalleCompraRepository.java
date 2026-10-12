package com.inventrack.inventrack.repository;

import com.inventrack.inventrack.entity.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {
    List<DetalleCompra> findByOrdenCompra_Id(Integer idCompra);
}