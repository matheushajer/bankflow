package com.bankflow.infrastructure.adapter.out.persistence;

import com.bankflow.infrastructure.adapter.out.persistence.entity.ContaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Detalhe de implementacao do adapter JPA - por isso package-private. Nao
 * deve ser referenciada fora deste pacote; quem precisa persistir Conta usa
 * a porta {@code ContaRepository} (application.port.out).
 */
interface ContaSpringDataRepository extends JpaRepository<ContaJpaEntity, UUID> {

    Optional<ContaJpaEntity> findByNumero(String numero);

    List<ContaJpaEntity> findByClienteId(UUID clienteId);

}
