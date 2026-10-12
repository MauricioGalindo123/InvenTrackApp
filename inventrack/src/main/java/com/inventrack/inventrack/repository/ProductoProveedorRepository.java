package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.ProductoProveedor;

public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, Integer> {

    boolean existsByProducto_IdAndProveedor_Id(Integer idProducto, Integer idProveedor);
}
