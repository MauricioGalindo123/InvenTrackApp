package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}