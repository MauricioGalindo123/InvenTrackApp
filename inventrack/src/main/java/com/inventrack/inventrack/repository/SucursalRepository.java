package com.inventrack.inventrack.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Sucursal;

public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {
}
