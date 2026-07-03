package com.salvador.quispe.repository;

import com.salvador.quispe.model.Alquiler;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AlquilerRepository extends ReactiveCrudRepository<Alquiler, UUID> {

    Flux<Alquiler> findByClienteId(String clienteId);

    Flux<Alquiler> findByVehiculoId(String vehiculoId);
}
