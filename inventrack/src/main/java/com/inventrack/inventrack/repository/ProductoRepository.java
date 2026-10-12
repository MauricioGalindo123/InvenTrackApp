package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    boolean existsByCategoria_Id(Integer idCategoria);

    boolean existsByCodigo(String codigo);
}