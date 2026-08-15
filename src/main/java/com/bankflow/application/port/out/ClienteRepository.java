package com.bankflow.application.port.out;

import com.bankflow.domain.model.Cliente;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para persistencia de {@link Cliente}. Trabalha sempre com a
 * entidade de dominio - quem implementa (adapter JPA, em
 * infrastructure/adapter/out/persistence) e responsavel por converter de/para
 * a entidade de persistencia.
 */
public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorCpf(String cpf);

}
