package com.salvador.quispe.controller;

import com.salvador.quispe.model.Alquiler;
import com.salvador.quispe.service.AlquilerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/alquileres")
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @GetMapping
    public Flux<Alquiler> listar() {
        return alquilerService.listar();
    }

    @GetMapping("/{id}")
    public Mono<Alquiler> obtenerPorId(@PathVariable UUID id) {
        return alquilerService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Alquiler> crear(@Valid @RequestBody Alquiler alquiler) {
        return alquilerService.crear(alquiler);
    }

    @PutMapping("/{id}")
    public Mono<Alquiler> actualizar(@PathVariable UUID id, @Valid @RequestBody Alquiler alquiler) {
        return alquilerService.actualizar(id, alquiler);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminar(@PathVariable UUID id) {
        return alquilerService.eliminar(id);
    }
}
