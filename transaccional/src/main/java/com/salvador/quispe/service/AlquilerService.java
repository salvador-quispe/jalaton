package com.salvador.quispe.service;

import com.salvador.quispe.client.ClienteClient;
import com.salvador.quispe.model.Alquiler;
import com.salvador.quispe.repository.AlquilerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class AlquilerService {

    private final AlquilerRepository alquilerRepository;
    private final ClienteClient clienteClient;

    public AlquilerService(AlquilerRepository alquilerRepository, ClienteClient clienteClient) {
        this.alquilerRepository = alquilerRepository;
        this.clienteClient = clienteClient;
    }

    public Flux<Alquiler> listar() {
        return alquilerRepository.findAll();
    }

    public Mono<Alquiler> obtenerPorId(UUID id) {
        return alquilerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado: " + id)));
    }

    public Mono<Alquiler> crear(Alquiler alquiler) {
        return clienteClient.obtenerCliente(alquiler.getClienteId())
                .flatMap(cliente -> {
                    alquiler.setId(null);
                    return alquilerRepository.save(alquiler);
                });
    }

    public Mono<Alquiler> actualizar(UUID id, Alquiler alquiler) {
        return obtenerPorId(id)
                .flatMap(existente -> {
                    existente.setClienteId(alquiler.getClienteId());
                    existente.setVehiculoId(alquiler.getVehiculoId());
                    existente.setDias(alquiler.getDias());
                    existente.setFechaInicio(alquiler.getFechaInicio());
                    existente.setFechaFin(alquiler.getFechaFin());
                    existente.setTotal(alquiler.getTotal());
                    existente.setEstado(alquiler.getEstado());
                    return alquilerRepository.save(existente);
                });
    }

    public Mono<Void> eliminar(UUID id) {
        return obtenerPorId(id)
                .flatMap(alquilerRepository::delete);
    }
}
