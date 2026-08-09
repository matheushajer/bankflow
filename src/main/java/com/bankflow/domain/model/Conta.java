package com.bankflow.domain.model;

import com.bankflow.domain.exception.ContaNaoAtivaException;
import com.bankflow.domain.exception.SaldoInsuficienteException;
import com.bankflow.domain.exception.ValorInvalidoException;
import com.bankflow.domain.model.enums.StatusConta;
import com.bankflow.domain.model.enums.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entidade de dominio Conta. Java puro (sem anotacoes de framework) - as
 * regras de negocio ficam aqui, nao na camada de aplicacao ou infraestrutura.
 * <p>
 * Regras que ContaTest espera (ver domain/exception para as excecoes prontas):
 * <ul>
 * - abrir(): limiteChequeEspecial nao pode ser negativo (IllegalArgumentException);
 *   conta nasce com saldo ZERO e status ATIVA; numero da conta nao pode ser vazio.
 * - depositar(): conta precisa estar ATIVA (ContaNaoAtivaException); valor
 *   precisa ser > 0 (ValorInvalidoException).
 * - sacar(): mesmas validacoes de depositar(), mais: saldo - valor nao pode
 *   ficar menor que -limiteChequeEspecial (SaldoInsuficienteException).
 * - bloquear(): muda o status para BLOQUEADA.
 * </ul>
 */
public class Conta {

    private static final String AGENCIA = "0001";

    private final UUID id;
    private final String numero;
    private final UUID clienteID;
    private final TipoConta tipoConta;
    private BigDecimal saldo = BigDecimal.ZERO;
    private BigDecimal limiteChequeEspecial;
    private StatusConta status;
    private final LocalDateTime dataCriacao;

    private Conta(UUID id, String numero, UUID clienteId, TipoConta tipoConta, BigDecimal limiteChequeEspecial,
                  StatusConta status, LocalDateTime dataCriacao) {
        this.id = id;
        this.numero = numero;
        this.clienteID = clienteId;
        this.tipoConta = tipoConta;
        this.limiteChequeEspecial = limiteChequeEspecial;
        this.status = status;
        this.dataCriacao = dataCriacao;

    }

    public static Conta abrir(UUID clienteId, TipoConta tipo, BigDecimal limiteChequeEspecial) {

        validarLimiteChequeEspecial(limiteChequeEspecial);

        return new Conta(UUID.randomUUID(), gerarNumeroContaAleatorio(), clienteId, tipo, limiteChequeEspecial, StatusConta.ATIVA, LocalDateTime.now());

    }

    private static String gerarNumeroContaAleatorio() {
        // Gera um número aleatório de 5 dígitos (entre 10000 e 99999)
        int numeroBase = ThreadLocalRandom.current().nextInt(10000, 100000);

        // Calcula um dígito verificador simples baseado no número base
        int digitoVerificador = calcularDigitoConta(numeroBase);

        return numeroBase + "-" + digitoVerificador;
    }

    private static int calcularDigitoConta(int numeroBase) {
        String s = String.valueOf(numeroBase);
        int soma = 0;
        // Exemplo de peso decrescente para o cálculo do DV da conta
        for (int i = 0; i < s.length(); i++) {
            int digito = s.charAt(i) - '0';
            soma += digito * (s.length() + 1 - i);
        }
        int resto = soma % 11;
        return resto == 10 ? 0 : resto;
    }

    public void depositar(BigDecimal valorDepositado) {

        if (valorDepositado == null || valorDepositado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("O valor de deposito não pode ser negativo ou zero");
        }

        if (!this.status.equals(StatusConta.ATIVA)) {
            throw new ContaNaoAtivaException("Conta com status " + this.status + ", deposito inválido!");
        }

        this.saldo = this.saldo.add(valorDepositado);
    }

    public void sacar(BigDecimal valorSacado) {

        if (valorSacado == null || valorSacado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoException("Valor invalido para saque!");
        }

        if (!this.status.equals(StatusConta.ATIVA)) {
            throw new ContaNaoAtivaException("Conta com status " + this.status + ", saque inválido!");
        }

        validarSaqueComChequeEspecial(valorSacado);

        this.saldo = this.saldo.subtract(valorSacado);

    }

    private void validarSaqueComChequeEspecial(BigDecimal valorSacado) {

        BigDecimal saldoFuturo = this.saldo.subtract(valorSacado);

        BigDecimal limiteNegativoPermitido = this.limiteChequeEspecial.negate();

        if (saldoFuturo.compareTo(limiteNegativoPermitido) < 0) {
            throw new SaldoInsuficienteException("Valor de saque abaixo do limite do cheque especial");
        }

    }

    public void bloquear() {
        this.status = StatusConta.BLOQUEADA;
    }

    private static void validarLimiteChequeEspecial(BigDecimal limiteChequeEspecial) {

        if (limiteChequeEspecial == null || limiteChequeEspecial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Limite para cheque especial não pode ser negativo!");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getAgencia() {
        return AGENCIA;
    }

    public UUID getClienteId() {
        return clienteID;
    }

    public TipoConta getTipoConta() {
        return tipoConta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getLimiteChequeEspecial() {
        return limiteChequeEspecial;
    }

    public StatusConta getStatus() {
        return status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

}
