package com.bankflow.infrastructure.adapter.out.persistence;

import com.bankflow.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Detalhe de implementacao do adapter JPA - por isso package-private. Nao
 * deve ser referenciada fora deste pacote; quem precisa persistir Cliente
 * usa a porta {@code ClienteRepository} (application.port.out).
 */
interface ClienteSpringDataRepository extends JpaRepository<ClienteJpaEntity, UUID> {

    Optional<ClienteJpaEntity> findByCpf(String cpf);

}
