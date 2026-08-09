package com.bankflow.domain.model;

import com.bankflow.domain.exception.ContaNaoAtivaException;
import com.bankflow.domain.exception.SaldoInsuficienteException;
import com.bankflow.domain.exception.ValorInvalidoException;
import com.bankflow.domain.model.enums.StatusConta;
import com.bankflow.domain.model.enums.TipoConta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seguindo o mesmo padrao de ClienteTest: JUnit 5 + AssertJ, um @Nested por
 * contexto, comentarios dado/quando/entao. Estes testes estao VERMELHOS de
 * proposito - implemente Conta (em domain/model/Conta.java) ate todos
 * passarem, um de cada vez.
 */
@DisplayName("Conta")
class ContaTest {

    private static final UUID CLIENTE_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Abertura de conta")
    class AberturaDeConta {

        @Test
        @DisplayName("deve abrir conta corrente ativa com saldo zero")
        void deveAbrirContaCorrenteAtivaComSaldoZero() {
            // dado
            BigDecimal limiteChequeEspecial = new BigDecimal("500.00");

            // quando
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, limiteChequeEspecial);

            // entao
            assertThat(conta.getClienteId()).isEqualTo(CLIENTE_ID);
            assertThat(conta.getTipoConta()).isEqualTo(TipoConta.CORRENTE);
            assertThat(conta.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(conta.getLimiteChequeEspecial()).isEqualByComparingTo(limiteChequeEspecial);
            assertThat(conta.getStatus()).isEqualTo(StatusConta.ATIVA);
            assertThat(conta.getNumero()).isNotBlank();
        }

        @Test
        @DisplayName("nao deve abrir conta com limite de cheque especial negativo")
        void naoDeveAbrirContaComLimiteNegativo() {
            // dado
            BigDecimal limiteNegativo = new BigDecimal("-100.00");

            // quando / entao
            assertThatThrownBy(() -> Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, limiteNegativo))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Deposito")
    class Deposito {

        @Test
        @DisplayName("deve aumentar o saldo ao depositar valor valido")
        void deveAumentarSaldoAoDepositarValorValido() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);

            // quando
            conta.depositar(new BigDecimal("150.00"));

            // entao
            assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("deve lancar excecao ao depositar valor zero ou negativo")
        void deveLancarExcecaoAoDepositarValorInvalido() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);

            // quando / entao
            assertThatThrownBy(() -> conta.depositar(BigDecimal.ZERO))
                    .isInstanceOf(ValorInvalidoException.class);

            assertThatThrownBy(() -> conta.depositar(new BigDecimal("-10.00")))
                    .isInstanceOf(ValorInvalidoException.class);
        }

        @Test
        @DisplayName("deve lancar excecao ao depositar em conta bloqueada")
        void deveLancarExcecaoAoDepositarEmContaBloqueada() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);
            conta.bloquear();

            // quando / entao
            assertThatThrownBy(() -> conta.depositar(new BigDecimal("50.00")))
                    .isInstanceOf(ContaNaoAtivaException.class);
        }
    }

    @Nested
    @DisplayName("Saque")
    class Saque {

        @Test
        @DisplayName("deve diminuir o saldo ao sacar valor dentro do saldo disponivel")
        void deveDiminuirSaldoAoSacarValorValido() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);
            conta.depositar(new BigDecimal("200.00"));

            // quando
            conta.sacar(new BigDecimal("80.00"));

            // entao
            assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("120.00"));
        }

        @Test
        @DisplayName("deve permitir saldo negativo dentro do limite de cheque especial")
        void devePermitirSaldoNegativoDentroDoLimite() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, new BigDecimal("100.00"));

            // quando
            conta.sacar(new BigDecimal("50.00"));

            // entao
            assertThat(conta.getSaldo()).isEqualByComparingTo(new BigDecimal("-50.00"));
        }

        @Test
        @DisplayName("deve lancar excecao ao sacar valor acima do saldo mais o limite")
        void deveLancarExcecaoAoSacarAcimaDoLimite() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, new BigDecimal("100.00"));

            // quando / entao
            assertThatThrownBy(() -> conta.sacar(new BigDecimal("150.01")))
                    .isInstanceOf(SaldoInsuficienteException.class);
        }

        @Test
        @DisplayName("deve lancar excecao ao sacar valor zero ou negativo")
        void deveLancarExcecaoAoSacarValorInvalido() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);

            // quando / entao
            assertThatThrownBy(() -> conta.sacar(BigDecimal.ZERO))
                    .isInstanceOf(ValorInvalidoException.class);
        }

        @Test
        @DisplayName("deve lancar excecao ao sacar de conta bloqueada")
        void deveLancarExcecaoAoSacarDeContaBloqueada() {
            // dado
            Conta conta = Conta.abrir(CLIENTE_ID, TipoConta.CORRENTE, BigDecimal.ZERO);
            conta.depositar(new BigDecimal("100.00"));
            conta.bloquear();

            // quando / entao
            assertThatThrownBy(() -> conta.sacar(new BigDecimal("10.00")))
                    .isInstanceOf(ContaNaoAtivaException.class);
        }
    }

}
