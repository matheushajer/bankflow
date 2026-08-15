package com.bankflow.infrastructure.adapter.out.persistence;

import com.bankflow.application.port.out.ClienteRepository;
import com.bankflow.domain.model.Cliente;
import com.bankflow.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementacao JPA da porta {@link ClienteRepository}. Converte entre a
 * entidade de dominio {@link Cliente} e a entidade de persistencia
 * {@link ClienteJpaEntity}; nenhuma das duas camadas ve a outra diretamente.
 * <p>
 * Nota: como o id de {@link Cliente} e atribuido no dominio (nao via
 * {@code @GeneratedValue}), o {@code save()} do Spring Data nao consegue
 * inferir se a entidade e nova so pelo id ser nao-nulo - por isso ele sempre
 * chama {@code merge()} (um SELECT extra antes do INSERT/UPDATE) em vez de
 * {@code persist()}. Funciona corretamente, mas e um ponto de atencao para
 * quem for otimizar isso depois (ver {@code Persistable<UUID>}).
 */
@Component
class ClienteRepositoryJpaAdapter implements ClienteRepository {

    private final ClienteSpringDataRepository springDataRepository;

    ClienteRepositoryJpaAdapter(ClienteSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        ClienteJpaEntity entidadeSalva = springDataRepository.save(paraEntity(cliente));
        return paraDominio(entidadeSalva);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<Cliente> buscarPorCpf(String cpf) {
        return springDataRepository.findByCpf(cpf).map(this::paraDominio);
    }

    private ClienteJpaEntity paraEntity(Cliente cliente) {
        return new ClienteJpaEntity(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpf(),
                cliente.getDataNascimento(),
                cliente.getEmail(),
                cliente.getStatus());
    }

    private Cliente paraDominio(ClienteJpaEntity entity) {
        return Cliente.restaurar(
                entity.getId(),
                entity.getNome(),
                entity.getCpf(),
                entity.getDataNascimento(),
                entity.getEmail(),
                entity.getStatus());
    }

}
