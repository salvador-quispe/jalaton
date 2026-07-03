package com.salvador.quispe.repository;

import com.salvador.quispe.model.Cliente;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClienteRepository extends ReactiveCrudRepository<Cliente, UUID> {

    Mono<Cliente> findByDni(String dni);

    Mono<Boolean> existsByDni(String dni);
}
