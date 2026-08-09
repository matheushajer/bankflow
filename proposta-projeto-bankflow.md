# BankFlow — Projeto de Estudo para Retomada de Java Puro

**Contexto:** Projeto pensado para simular o dia a dia de um Desenvolvedor Backend Pleno atuando em cliente bancário. Foco em Java "na mão" (sem depender só de testes), arquitetura hexagonal, Spring Boot, SQL Server e Redis.

---

## 1. Visão geral do domínio

Vamos construir um **núcleo de conta digital** (Core Banking simplificado), cobrindo os fluxos mais comuns que aparecem em bancos digitais e que costumam cair em entrevistas/dia a dia: abertura de conta, movimentações financeiras, transferências (estilo PIX), limites, tarifação e extrato.

---

## 2. Arquitetura da solução

### 2.1 Estilo arquitetural
**Arquitetura Hexagonal (Ports & Adapters)**, dentro de uma estrutura que evolui em fases (ver seção 6):

```
                        ┌─────────────────────────┐
                        │   Adapters de Entrada    │
                        │  (REST Controllers,      │
                        │   Kafka Listener, etc)   │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │      Portas de Entrada    │
                        │   (Use Cases / Services)  │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │        Domínio            │
                        │  (Entidades, VOs, Regras) │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │      Portas de Saída      │
                        │  (Repository Interfaces)  │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │   Adapters de Saída       │
                        │ (JPA/SQL Server, Redis,   │
                        │  Kafka Producer, etc)     │
                        └───────────────────────────┘
```

### 2.2 Divisão de serviços (evolução para microsserviços)
Fase inicial em **monólito modular** e depois quebrado em:

| Serviço | Responsabilidade |
|---|---|
| `account-service` | Cadastro de cliente, abertura/encerramento de conta, saldo |
| `transaction-service` | Depósitos, saques, transferências, estorno |
| `limit-service` | Limites diários, regras antifraude simples |
| `notification-service` (opcional) | Simula notificação de eventos (log/console/e-mail fake) |

### 2.3 Stack técnica
- **Java 21** + **Spring Boot 3**
- **Spring Data JPA** + **SQL Server**
- **Redis** — cache de saldo, controle de idempotência, rate limiting de transações
- **Flyway** — versionamento de schema (prática comum em bancos)
- **JUnit 5 + Mockito + Testcontainers** — já é seu ponto forte, use para validar o domínio que você vai escrever
- **Docker Compose** — subir SQL Server + Redis localmente
- **Kafka** (fase avançada, opcional) — eventos de transação para o notification-service
- **Spring Security + JWT** (fase avançada)

---

## 3. Entidades e dados

### 3.1 `Cliente`
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| nome | String | |
| cpf | String | único, validado (dígito verificador) |
| dataNascimento | LocalDate | usado para regra de maioridade |
| email | String | único |
| status | Enum (`ATIVO`, `BLOQUEADO`, `INATIVO`) | |

### 3.2 `Conta`
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| numero | String | gerado, único |
| agencia | String | fixo "0001" para simplificar |
| clienteId | UUID | FK |
| tipo | Enum (`CORRENTE`, `POUPANCA`) | |
| saldo | BigDecimal | **nunca use double para dinheiro** |
| limiteChequeEspecial | BigDecimal | |
| status | Enum (`ATIVA`, `BLOQUEADA`, `ENCERRADA`) | |
| dataAbertura | LocalDateTime | |

### 3.3 `Transacao`
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| contaOrigemId | UUID | nulo em depósito |
| contaDestinoId | UUID | nulo em saque |
| tipo | Enum (`DEPOSITO`, `SAQUE`, `TRANSFERENCIA`, `ESTORNO`) | |
| valor | BigDecimal | > 0 |
| status | Enum (`PENDENTE`, `CONCLUIDA`, `FALHA`, `ESTORNADA`) | |
| chaveIdempotencia | String | único — evita transação duplicada |
| dataHora | LocalDateTime | |
| descricao | String | opcional |

### 3.4 `LimiteDiario`
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| contaId | UUID | FK |
| data | LocalDate | dia de referência |
| valorMovimentadoTransferencia | BigDecimal | soma do dia |
| valorMovimentadoSaque | BigDecimal | soma do dia |

### 3.5 `TarifaAplicada` (fase avançada)
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| contaId | UUID | FK |
| transacaoId | UUID | FK |
| valor | BigDecimal | |
| motivo | String | ex: "TED fora do limite gratuito" |

---

## 4. Regras de negócio

Estas regras são o coração do exercício.

**Abertura de conta**
- Cliente precisa ter 18 anos ou mais.
- CPF deve ser válido..
- Um cliente pode ter no máximo 1 conta corrente e 1 poupança.

**Depósito**
- Valor deve ser > 0.
- Conta precisa estar `ATIVA`.

**Saque**
- Não pode deixar saldo menor que `-limiteChequeEspecial`.
- Respeita limite diário de saque.

**Transferência**
- Débito na origem e crédito no destino devem ser **atômicos** (mesma transação de banco de dados).
- Verifica limite diário de transferência.
- Se a conta destino não existir ou estiver bloqueada, a transação falha e nada é debitado.
- Usa `chaveIdempotencia` para garantir que reenvio da mesma requisição não duplique a transferência (ótimo exercício de uso do Redis: `SETNX` com TTL).

**Estorno**
- Só é possível estornar transação `CONCLUIDA` dentro de X minutos (ex: 30 min).
- Gera uma nova transação do tipo `ESTORNO`, não apaga a original (rastreabilidade é chave em banco).

**Bloqueio por suspeita de fraude (regra simples)**
- Mais de N transferências acima de determinado valor no mesmo dia → conta é sinalizada e bloqueada automaticamente, exigindo "desbloqueio manual" (simulado por um endpoint admin).

**Concorrência**
- Dois saques simultâneos na mesma conta não podem levar o saldo a um estado inconsistente. Isso é proposital: te força a estudar `@Version` (optimistic locking) ou lock pessimista, tema clássico de entrevista.

---

## 5. Endpoints sugeridos (REST)

```
POST   /clientes
GET    /clientes/{id}

POST   /contas
GET    /contas/{id}
GET    /contas/{id}/extrato?inicio=&fim=

POST   /transacoes/deposito
POST   /transacoes/saque
POST   /transacoes/transferencia   (Header: Idempotency-Key)
POST   /transacoes/{id}/estorno

GET    /transacoes/{id}
```

---

## 6. Roteiro de evolução (fases)

| Fase | Foco | Duração sugerida |
|---|---|---|
| **1 — Núcleo do domínio** | Modelar entidades, regras de negócio em Java puro, cobrir com testes unitários (seu ponto forte) *antes* de qualquer framework | 1 semana |
| **2 — Persistência** | Plugar Spring Data JPA + SQL Server, migrations com Flyway, mapear agregados corretamente | 1 semana |
| **3 — API REST + Hexagonal** | Expor casos de uso via controllers, organizar pacotes em `domain`, `application`, `infrastructure` | 1 semana |
| **4 — Redis** | Idempotência de transferência, cache de saldo, rate limit de tentativas de saque | 3-4 dias |
| **5 — Concorrência e consistência** | Testar cenários de corrida com Testcontainers, aplicar locking | 3-4 dias |
| **6 — Segurança** | Spring Security + JWT, perfis (cliente comum vs admin) | 3-4 dias |
| **7 — Quebra em microsserviços** | Separar `account-service` e `transaction-service`, comunicação via REST ou Kafka | 1-2 semanas |
| **8 — Observabilidade e CI/CD** | Actuator, Docker Compose, pipeline simples (GitHub Actions) | 3-4 dias |

---
