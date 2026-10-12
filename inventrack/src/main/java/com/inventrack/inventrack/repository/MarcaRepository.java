package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Marca;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {
}