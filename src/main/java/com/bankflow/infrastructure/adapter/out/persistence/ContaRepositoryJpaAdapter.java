package com.bankflow.infrastructure.adapter.out.persistence;

import com.bankflow.application.port.out.ContaRepository;
import com.bankflow.domain.exception.ContaDesatualizadaException;
import com.bankflow.domain.model.Conta;
import com.bankflow.infrastructure.adapter.out.persistence.entity.ContaJpaEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementacao JPA da porta {@link ContaRepository}. Converte entre a
 * entidade de dominio {@link Conta} e a entidade de persistencia
 * {@link ContaJpaEntity}; nenhuma das duas camadas ve a outra diretamente.
 * <p>
 * A agencia gravada em banco vem sempre de {@link Conta#getAgencia()} (que e
 * a constante "0001" da classe de dominio); ao reidratar, ela nao e
 * repassada para {@link Conta#restaurar}, ja que nao existe como campo de
 * instancia no dominio.
 * <p>
 * Nota: como o id de {@link Conta} e atribuido no dominio (nao via
 * {@code @GeneratedValue}), o {@code save()} do Spring Data nao consegue
 * inferir se a entidade e nova so pelo id ser nao-nulo - por isso ele sempre
 * chama {@code merge()} (um SELECT extra antes do INSERT/UPDATE) em vez de
 * {@code persist()}, inclusive para conta nova. Mesma situacao documentada em
 * {@code ClienteRepositoryJpaAdapter}; funciona corretamente, mas e um ponto
 * de atencao para quem for otimizar isso depois (ver {@code Persistable<UUID>}).
 */
@Component
class ContaRepositoryJpaAdapter implements ContaRepository {

    private final ContaSpringDataRepository springDataRepository;

    ContaRepositoryJpaAdapter(ContaSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /**
     * Contrato transacional / lock otimista: {@code Conta} carrega, de forma
     * opaca, a {@code version} que leu do banco (ver
     * {@link Conta#getVersion()} e {@link Conta#restaurar}). Aqui montamos
     * uma {@link ContaJpaEntity} nova levando essa mesma versao "antiga"
     * junto e mandamos pro {@code save()} (que cai em
     * {@code entityManager.merge()}, ja que o id e atribuido no dominio, nao
     * gerado pelo banco).
     * <p>
     * O merge de uma entidade versionada faz o Hibernate comparar a versao
     * que veio com a entidade contra a versao ATUAL da linha no banco antes
     * de aplicar o UPDATE: se baterem, o update segue e a versao e
     * incrementada; se nao baterem - ou seja, alguem gravou por cima entre a
     * leitura desta Conta e este {@code salvar()} - o Hibernate lanca
     * {@code OptimisticLockException}, que o Spring traduz para
     * {@code ObjectOptimisticLockingFailureException}. Esse tipo e especifico
     * de Spring/JPA, entao NAO deve vazar pela porta {@link ContaRepository}
     * (que e agnostica de framework) - por isso capturamos aqui e relancamos
     * como {@link ContaDesatualizadaException}, que e o tipo que quem chama
     * {@code salvar()} deve conhecer e tratar (ver teste de concorrencia em
     * {@code ContaRepositoryJpaAdapterIT}).
     * <p>
     * O {@code @Transactional} aqui delimita o merge + flush como uma unica
     * unidade atomica: sem ele, o {@code save()} do Spring Data ainda abriria
     * sua propria transacao internamente (o mesmo efeito pratico para este
     * metodo isolado), mas deixamos explicito porque qualquer logica futura
     * adicionada a este metodo (auditoria, outro repositorio, etc.) precisa
     * continuar dentro da mesma transacao para nao quebrar essa garantia.
     * <p>
     * Importante: isto so protege contra concorrencia se quem chama
     * {@code salvar()} sempre partir de uma {@code Conta} obtida via
     * {@link #buscarPorId} (ou equivalente) pouco antes - nunca de uma copia
     * antiga guardada em memoria por muito tempo. Em caso de
     * {@link ContaDesatualizadaException}, o caminho correto e buscar a
     * conta de novo e reaplicar a operacao, nunca tentar salvar de novo com
     * o mesmo objeto.
     */
    @Override
    @Transactional
    public Conta salvar(Conta conta) {
        try {
            ContaJpaEntity entidadeSalva = springDataRepository.save(paraEntity(conta));
            return paraDominio(entidadeSalva);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ContaDesatualizadaException(
                    "Conta " + conta.getId() + " foi alterada por outra operacao entre a leitura e este salvamento; "
                            + "busque a conta novamente e reaplique a operacao.", e);
        }
    }

    @Override
    public Optional<Conta> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<Conta> buscarPorNumero(String numero) {
        return springDataRepository.findByNumero(numero).map(this::paraDominio);
    }

    @Override
    public List<Conta> buscarPorClienteId(UUID clienteId) {
        return springDataRepository.findByClienteId(clienteId).stream()
                .map(this::paraDominio)
                .collect(Collectors.toList());
    }

    private ContaJpaEntity paraEntity(Conta conta) {
        return new ContaJpaEntity(
                conta.getId(),
                conta.getClienteId(),
                conta.getNumero(),
                conta.getAgencia(),
                conta.getTipoConta(),
                conta.getSaldo(),
                conta.getLimiteChequeEspecial(),
                conta.getStatus(),
                conta.getDataCriacao(),
                conta.getVersion());
    }

    private Conta paraDominio(ContaJpaEntity entity) {
        return Conta.restaurar(
                entity.getId(),
                entity.getNumero(),
                entity.getClienteId(),
                entity.getTipo(),
                entity.getSaldo(),
                entity.getLimiteChequeEspecial(),
                entity.getStatus(),
                entity.getDataAbertura(),
                entity.getVersion());
    }

}
