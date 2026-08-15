package com.bankflow.infrastructure.adapter.out.persistence;

import com.bankflow.application.port.out.ClienteRepository;
import com.bankflow.application.port.out.ContaRepository;
import com.bankflow.domain.exception.ContaDesatualizadaException;
import com.bankflow.domain.model.Cliente;
import com.bankflow.domain.model.Conta;
import com.bankflow.domain.model.enums.StatusConta;
import com.bankflow.domain.model.enums.TipoConta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integracao real com SQL Server via Testcontainers (nada de H2 ou
 * mocks) - sobe o container, roda as migrations Flyway de verdade e exercita
 * o adapter contra um banco real. Mesmo padrao de ContaTest: JUnit 5 +
 * AssertJ, @Nested por contexto, comentarios dado/quando/entao.
 * <p>
 * Fica no mesmo pacote do adapter (infrastructure.adapter.out.persistence)
 * por convencao de teste de integracao, mas depende apenas das portas
 * publicas (ClienteRepository / ContaRepository), nunca das entidades JPA
 * ou das interfaces Spring Data diretamente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ContaRepositoryJpaAdapterIT {

    @Container
    static MSSQLServerContainer<?> mssqlContainer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mssqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mssqlContainer::getUsername);
        registry.add("spring.datasource.password", mssqlContainer::getPassword);
    }

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ContaRepository contaRepository;

    private Cliente criarClientePersistido() {
        Cliente cliente = Cliente.criar("Maria Silva", "529.982.247-25", LocalDate.now().minusYears(30), "maria@email.com");
        return clienteRepository.salvar(cliente);
    }

    @Nested
    @DisplayName("Salvar e buscar por id")
    class SalvarEBuscarPorId {

        @Test
        @DisplayName("deve salvar conta vinculada a cliente existente e recuperar os mesmos dados por id")
        void deveSalvarContaVinculadaAClienteERecuperarPorId() {
            // dado
            Cliente cliente = criarClientePersistido();
            Conta conta = Conta.abrir(cliente.getId(), TipoConta.CORRENTE, new BigDecimal("300.00"));
            conta.depositar(new BigDecimal("150.00"));

            // quando
            Conta contaSalva = contaRepository.salvar(conta);
            Optional<Conta> contaEncontrada = contaRepository.buscarPorId(contaSalva.getId());

            // entao
            assertThat(contaEncontrada).isPresent();
            Conta encontrada = contaEncontrada.get();
            assertThat(encontrada.getId()).isEqualTo(conta.getId());
            assertThat(encontrada.getClienteId()).isEqualTo(cliente.getId());
            assertThat(encontrada.getNumero()).isEqualTo(conta.getNumero());
            assertThat(encontrada.getTipoConta()).isEqualTo(TipoConta.CORRENTE);
            assertThat(encontrada.getSaldo()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(encontrada.getLimiteChequeEspecial()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(encontrada.getStatus()).isEqualTo(StatusConta.ATIVA);
            assertThat(encontrada.getDataCriacao()).isNotNull();
        }

        @Test
        @DisplayName("nao deve encontrar conta com id inexistente")
        void naoDeveEncontrarContaComIdInexistente() {
            // dado / quando
            Optional<Conta> contaEncontrada = contaRepository.buscarPorId(java.util.UUID.randomUUID());

            // entao
            assertThat(contaEncontrada).isEmpty();
        }
    }

    @Nested
    @DisplayName("Buscar por numero")
    class BuscarPorNumero {

        @Test
        @DisplayName("deve encontrar conta pelo numero")
        void deveEncontrarContaPeloNumero() {
            // dado
            Cliente cliente = criarClientePersistido();
            Conta conta = Conta.abrir(cliente.getId(), TipoConta.POUPANCA, BigDecimal.ZERO);
            contaRepository.salvar(conta);

            // quando
            Optional<Conta> contaEncontrada = contaRepository.buscarPorNumero(conta.getNumero());

            // entao
            assertThat(contaEncontrada).isPresent();
            assertThat(contaEncontrada.get().getNumero()).isEqualTo(conta.getNumero());
            assertThat(contaEncontrada.get().getTipoConta()).isEqualTo(TipoConta.POUPANCA);
        }

        @Test
        @DisplayName("nao deve encontrar conta com numero inexistente")
        void naoDeveEncontrarContaComNumeroInexistente() {
            // dado / quando
            Optional<Conta> contaEncontrada = contaRepository.buscarPorNumero("00000-0");

            // entao
            assertThat(contaEncontrada).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lock otimista em salvamentos concorrentes")
    class LockOtimista {

        @Test
        @DisplayName("segundo salvamento baseado em leitura desatualizada deve falhar com ContaDesatualizadaException")
        void deveFalharAoSalvarSegundaLeituraDesatualizadaDaMesmaConta() {
            // dado: conta persistida, depois lida DUAS vezes (simula duas transacoes/instancias
            // concorrentes que leram o mesmo estado antes de qualquer uma delas escrever)
            Cliente cliente = criarClientePersistido();
            Conta contaAberta = Conta.abrir(cliente.getId(), TipoConta.CORRENTE, BigDecimal.ZERO);
            contaRepository.salvar(contaAberta);

            Conta primeiraLeitura = contaRepository.buscarPorId(contaAberta.getId()).orElseThrow();
            Conta segundaLeitura = contaRepository.buscarPorId(contaAberta.getId()).orElseThrow();

            // quando: a primeira leitura deposita e salva com sucesso - avanca a versao no banco
            primeiraLeitura.depositar(new BigDecimal("100.00"));
            contaRepository.salvar(primeiraLeitura);

            // entao: a segunda leitura, ainda baseada na versao antiga, deve ser rejeitada ao
            // tentar salvar - nunca sobrescrever silenciosamente o deposito da primeira
            segundaLeitura.depositar(new BigDecimal("50.00"));
            assertThatThrownBy(() -> contaRepository.salvar(segundaLeitura))
                    .isInstanceOf(ContaDesatualizadaException.class);

            // e o saldo persistido deve refletir só o primeiro depósito, nunca o segundo
            Conta contaFinal = contaRepository.buscarPorId(contaAberta.getId()).orElseThrow();
            assertThat(contaFinal.getSaldo()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

}
