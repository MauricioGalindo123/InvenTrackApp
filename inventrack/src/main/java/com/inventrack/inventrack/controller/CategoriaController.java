package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.entity.Categoria;
import com.inventrack.inventrack.service.CategoriaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categoria")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria registrar(@RequestBody Categoria categoria) { return service.registrar(categoria); }

    @PutMapping("/actualizar")
    public Categoria actualizar(@RequestBody Categoria categoria) { return service.actualizar(categoria); }

    @GetMapping("/listar")
    public List<Categoria> listar() { return service.listar(); }

    @GetMapping("/buscar-por-id")
    public Categoria buscar(@RequestParam Integer idCategorias) { return service.buscar(idCategorias); }

    @DeleteMapping("/eliminar")
    public Map<String, String> eliminar(@RequestParam Integer idCategorias) {
        service.eliminar(idCategorias);
        return Map.of("mensaje", "Categoria eliminado");
    }
}