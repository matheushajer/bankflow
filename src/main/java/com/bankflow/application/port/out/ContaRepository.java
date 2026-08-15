package com.bankflow.application.port.out;

import com.bankflow.domain.exception.ContaDesatualizadaException;
import com.bankflow.domain.model.Conta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para persistencia de {@link Conta}. Trabalha sempre com a
 * entidade de dominio - quem implementa (adapter JPA, em
 * infrastructure/adapter/out/persistence) e responsavel por converter de/para
 * a entidade de persistencia.
 */
public interface ContaRepository {

    /**
     * Persiste o estado atual de {@code conta}, seja criando (conta nova,
     * de {@link Conta#abrir}) ou atualizando (conta existente, obtida antes
     * via {@link #buscarPorId} ou {@link #buscarPorNumero}).
     * <p>
     * Contrato de concorrencia: a implementacao deve fazer lock otimista -
     * se {@code conta} foi lida de um estado que ja nao e mais o mais
     * recente em persistencia (outra chamada a {@code salvar} alterou a
     * mesma conta nesse meio tempo), esta chamada deve falhar lancando
     * {@link ContaDesatualizadaException} em vez de sobrescrever
     * silenciosamente. Por isso quem usa esta porta (casos de uso de
     * deposito/saque, por exemplo) sempre deve partir de uma {@code Conta}
     * buscada pouco antes, e ao receber essa excecao, buscar de novo e
     * reaplicar a operacao - nunca insistir salvando o mesmo objeto em
     * memoria.
     */
    Conta salvar(Conta conta);

    Optional<Conta> buscarPorId(UUID id);

    Optional<Conta> buscarPorNumero(String numero);

    /**
     * Um cliente pode ter mais de uma conta (ex: corrente + poupanca), por
     * isso retorna lista em vez de Optional.
     */
    List<Conta> buscarPorClienteId(UUID clienteId);

}
