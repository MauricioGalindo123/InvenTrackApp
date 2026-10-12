package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.Marca;
import com.inventrack.inventrack.service.MarcaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/marca")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Marca registrar(@RequestBody Marca marca) { return service.registrar(marca); }

    @PutMapping("/actualizar")
    public Marca actualizar(@RequestBody Marca marca) { return service.actualizar(marca); }

    @GetMapping("/listar")
    public List<Marca> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Marca buscar(@RequestParam Integer idMarca) { return service.buscar(idMarca); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idMarca) {
        service.eliminar(idMarca);
        return Map.of("mensaje", "Marca eliminado");
    }
}