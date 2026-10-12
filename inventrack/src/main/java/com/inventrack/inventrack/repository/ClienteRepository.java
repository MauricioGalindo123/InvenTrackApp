package com.inventrack.inventrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
}