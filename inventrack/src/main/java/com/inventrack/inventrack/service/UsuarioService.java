package com.inventrack.inventrack.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.inventrack.inventrack.dto.LoginRequest;
import com.inventrack.inventrack.dto.UsuarioRequest;
import com.inventrack.inventrack.entity.Usuario;
import com.inventrack.inventrack.repository.SucursalRepository;
import com.inventrack.inventrack.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;
    private final SucursalRepository sucursalRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public Usuario registrar(UsuarioRequest r) {
        if (r.contrasena() == null || r.contrasena().isBlank()) {
            throw bad("La contraseña es obligatoria");
        }
        if (repo.findByCorreo(r.correo()).isPresent()) {
            throw bad("Ya existe un usuario con ese correo");
        }
        Usuario u = new Usuario();
        copiar(u, r);
        u.setContrasena(encoder.encode(r.contrasena()));
        u.setActivo(true);
        u.setFechaCreacion(LocalDateTime.now());
        return repo.save(u);
    }

    public Usuario actualizar(UsuarioRequest r) {
        Usuario u = buscar(r.idUsuario());
        repo.findByCorreo(r.correo()).ifPresent(otro -> {
            if (!otro.getId().equals(u.getId())) {
                throw bad("Ya existe un usuario con ese correo");
            }
        });
        copiar(u, r);
        if (r.contrasena() != null && !r.contrasena().isBlank()) {
            u.setContrasena(encoder.encode(r.contrasena()));
        }
        return repo.save(u);
    }

    public List<Usuario> listar() {
        return repo.findAll();
    }

    public Usuario buscar(Integer id) {
        if (id == null) {
            throw bad("El id es obligatorio");
        }
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    /** Baja lógica. */
    public void darDeBaja(Integer id) {
        Usuario u = buscar(id);
        u.setActivo(false);
        repo.save(u);
    }

    /** EP01: valida credenciales, actualiza el último acceso y devuelve el token JWT. */
    public Map<String, Object> login(LoginRequest r) {
        Usuario u = repo.findByCorreo(r.correo())
            .filter(x -> Boolean.TRUE.equals(x.getActivo()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        if (!encoder.matches(r.contrasena(), u.getContrasena())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        u.setAcceso(LocalDateTime.now());
        repo.save(u);
        return Map.of("idUsuario", u.getId(), "bearer", "Bearer", "nombreCompleto", u.getNombre(),
                      "token", jwtService.generar(u), "nombre", u.getNombre(),
                      "rol", u.getRol(), "idSucursal", u.getSucursal().getId());
    }

    private void copiar(Usuario u, UsuarioRequest r) {
        u.setSucursal(sucursalRepo.findById(r.idSucursal()).orElseThrow(() -> bad("La sucursal no existe")));
        u.setNombre(r.nombre());
        u.setCorreo(r.correo());
        u.setRol(r.rol());
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}