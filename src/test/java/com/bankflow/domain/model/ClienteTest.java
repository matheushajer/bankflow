package com.bankflow.domain.model;

import com.bankflow.domain.exception.ClienteMenorDeIdadeException;
import com.bankflow.domain.exception.CpfInvalidoException;
import com.bankflow.domain.model.enums.StatusCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Classe de REFERENCIA: este e o padrao de testes usado no projeto.
 * Use-a como modelo ao escrever as demais classes de dominio:
 * <p>
 * - JUnit 5 + AssertJ (assertThat / assertThatThrownBy);
 * - um @Nested por "contexto" (ex: criacao, mudanca de status);
 * - nomes de metodo no formato deveXxxQuandoYyy (ou deveXxx quando o contexto
 *   ja deixa o "quando" implicito, como aqui dentro dos @Nested);
 * - comentarios // dado // quando // entao demarcando as 3 fases do teste
 *   (Arrange-Act-Assert), mesmo quando alguma fase for so uma linha.
 */
@DisplayName("Cliente")
class ClienteTest {

    private static final String CPF_VALIDO = "529.982.247-25";
    private static final LocalDate DATA_NASCIMENTO_MAIOR_IDADE = LocalDate.now().minusYears(25);

    @Nested
    @DisplayName("Criacao de cliente")
    class CriacaoDeCliente {

        @Test
        @DisplayName("deve criar cliente ativo quando dados sao validos")
        void deveCriarClienteAtivoQuandoDadosSaoValidos() {
            // dado / quando
            Cliente cliente = Cliente.criar("Maria Silva", CPF_VALIDO, DATA_NASCIMENTO_MAIOR_IDADE, "maria@email.com");

            // entao
            assertThat(cliente.getId()).isNotNull();
            assertThat(cliente.getNome()).isEqualTo("Maria Silva");
            assertThat(cliente.getCpf()).isEqualTo("52998224725");
            assertThat(cliente.getStatus()).isEqualTo(StatusCliente.ATIVO);
        }

        @Test
        @DisplayName("nao deve criar cliente com CPF invalido")
        void naoDeveCriarClienteComCpfInvalido() {
            // dado / quando / entao
            assertThatThrownBy(() ->
                    Cliente.criar("Maria Silva", "111.111.111-11", DATA_NASCIMENTO_MAIOR_IDADE, "maria@email.com"))
                    .isInstanceOf(CpfInvalidoException.class);
        }

        @Test
        @DisplayName("nao deve criar cliente menor de idade")
        void naoDeveCriarClienteMenorDeIdade() {
            // dado
            LocalDate dataNascimentoMenorDeIdade = LocalDate.now().minusYears(17);

            // quando / entao
            assertThatThrownBy(() ->
                    Cliente.criar("Joao Menor", CPF_VALIDO, dataNascimentoMenorDeIdade, "joao@email.com"))
                    .isInstanceOf(ClienteMenorDeIdadeException.class);
        }

        @Test
        @DisplayName("nao deve criar cliente sem nome")
        void naoDeveCriarClienteSemNome() {
            // dado / quando / entao
            assertThatThrownBy(() ->
                    Cliente.criar(" ", CPF_VALIDO, DATA_NASCIMENTO_MAIOR_IDADE, "maria@email.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nao deve criar cliente com email invalido")
        void naoDeveCriarClienteComEmailInvalido() {
            // dado / quando / entao
            assertThatThrownBy(() ->
                    Cliente.criar("Maria Silva", CPF_VALIDO, DATA_NASCIMENTO_MAIOR_IDADE, "email-invalido"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Mudanca de status")
    class MudancaDeStatus {

        @Test
        @DisplayName("deve bloquear cliente ativo")
        void deveBloquearClienteAtivo() {
            // dado
            Cliente cliente = Cliente.criar("Maria Silva", CPF_VALIDO, DATA_NASCIMENTO_MAIOR_IDADE, "maria@email.com");

            // quando
            cliente.bloquear();

            // entao
            assertThat(cliente.getStatus()).isEqualTo(StatusCliente.BLOQUEADO);
        }

        @Test
        @DisplayName("deve reativar cliente bloqueado")
        void deveReativarClienteBloqueado() {
            // dado
            Cliente cliente = Cliente.criar("Maria Silva", CPF_VALIDO, DATA_NASCIMENTO_MAIOR_IDADE, "maria@email.com");
            cliente.bloquear();

            // quando
            cliente.ativar();

            // entao
            assertThat(cliente.getStatus()).isEqualTo(StatusCliente.ATIVO);
        }
    }

}
