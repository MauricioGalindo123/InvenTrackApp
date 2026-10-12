package com.inventrack.inventrack.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.inventrack.inventrack.dto.TrasladoRequest;
import com.inventrack.inventrack.entity.Traslado;
import com.inventrack.inventrack.service.TrasladoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/traslado")
@RequiredArgsConstructor
public class TrasladoController {

    private final TrasladoService service;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody TrasladoRequest r) {
        Traslado t = service.registrar(r);
        return Map.of("idTraslado", t.getId(), "estado", t.getEstado());
    }

    @PutMapping("/enviar")
    public Map<String, String> enviar(@RequestParam Integer idTraslado) {
        service.enviar(idTraslado);
        return Map.of("mensaje", "Traslado enviado y stock descontado en el origen");
    }

    @PutMapping("/recibir")
    public Map<String, String> recibir(@RequestParam Integer idTraslado) {
        service.recibir(idTraslado);
        return Map.of("mensaje", "Traslado recibido y stock agregado en el destino");
    }

    @GetMapping("/listar")
    public List<Traslado> listar(@RequestParam(required = false) Integer idSucursal,
                                 @RequestParam(required = false) String estado) {
        return service.listar(idSucursal, estado);
    }
}
