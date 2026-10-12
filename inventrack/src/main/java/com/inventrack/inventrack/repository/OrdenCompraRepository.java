package com.inventrack.inventrack.repository;

import com.inventrack.inventrack.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer> {
    List<OrdenCompra> findBySucursal_Id(Integer idSucursal);
    List<OrdenCompra> findByEstadoIgnoreCase(String estado);
    List<OrdenCompra> findBySucursal_IdAndEstadoIgnoreCase(Integer idSucursal, String estado);
}