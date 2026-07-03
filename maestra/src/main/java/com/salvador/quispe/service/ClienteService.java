package com.salvador.quispe.service;

import com.salvador.quispe.model.Cliente;
import com.salvador.quispe.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Flux<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Mono<Cliente> obtenerPorId(UUID id) {
        return clienteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado: " + id)));
    }

    public Mono<Cliente> crear(Cliente cliente) {
        return clienteRepository.existsByDni(cliente.getDni())
                .flatMap(existe -> {
                    if (Boolean.TRUE.equals(existe)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con DNI: " + cliente.getDni()));
                    }
                    cliente.setId(null);
                    return clienteRepository.save(cliente);
                });
    }

    public Mono<Cliente> actualizar(UUID id, Cliente cliente) {
        return obtenerPorId(id)
                .flatMap(existente -> {
                    existente.setDni(cliente.getDni());
                    existente.setNombres(cliente.getNombres());
                    existente.setApellidos(cliente.getApellidos());
                    existente.setCelular(cliente.getCelular());
                    existente.setCorreo(cliente.getCorreo());
                    existente.setLicencia(cliente.getLicencia());
                    existente.setEstado(cliente.getEstado());
                    return clienteRepository.save(existente);
                });
    }

    public Mono<Void> eliminar(UUID id) {
        return obtenerPorId(id)
                .flatMap(clienteRepository::delete);
    }
}
