package com.bankflow.domain.exception;

/**
 * Lancada quando uma {@code Conta} e salva a partir de uma leitura que ja
 * nao reflete mais o estado mais recente em persistencia - outra operacao
 * (deposito, saque, bloqueio) alterou a mesma conta entre a leitura e esta
 * escrita (ver {@code ContaRepository#salvar}).
 * <p>
 * E a traducao, em vocabulario de dominio, do conflito de lock otimista
 * detectado pela camada de persistencia. Quem chama {@code salvar()} nao
 * deve conhecer o tipo de excecao especifico do Spring/JPA que originou
 * isso - so que a conta estava desatualizada e a operacao precisa ser
 * reaplicada sobre uma leitura nova (buscar a conta de novo, refazer a
 * operacao de negocio, salvar de novo).
 */
public class ContaDesatualizadaException extends RuntimeException {

    public ContaDesatualizadaException(String message) {
        super(message);
    }

    /**
     * Preserva a excecao original (ex.: ObjectOptimisticLockingFailureException
     * do Spring) como causa, para nao perder a stack trace/diagnostico de
     * quem investigar um conflito de concorrencia em producao.
     */
    public ContaDesatualizadaException(String message, Throwable cause) {
        super(message, cause);
    }

}
