package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    boolean existsByRfcNit(String rfcNit);
}
