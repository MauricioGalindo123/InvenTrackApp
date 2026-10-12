package com.inventrack.inventrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.inventrack.inventrack.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);
}
