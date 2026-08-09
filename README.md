# BankFlow

Projeto de estudo — núcleo de conta digital (Core Banking simplificado) em
Java 21 + Spring Boot 3, arquitetura hexagonal, SQL Server e Redis.

Consulte `proposta-projeto-bankflow.md` (documento entregue à parte) para a
visão geral do domínio, entidades, regras de negócio e roteiro completo em
8 fases.

## Estrutura de pacotes

```
com.bankflow
├── domain
│   ├── model            -> Entidades e VOs, Java puro (Fase 1)
│   │   └── enums
│   └── exception         -> Exceções de negócio
├── application
│   ├── port
│   │   ├── in            -> Interfaces dos casos de uso
│   │   └── out           -> Interfaces para o mundo externo (repos, cache)
│   └── usecase            -> Implementação dos casos de uso
└── infrastructure
    ├── adapter
    │   ├── in/web         -> Controllers REST + DTOs (Fase 3)
    │   └── out
    │       ├── persistence -> Adapters JPA + entidades @Entity (Fase 2)
    │       └── cache        -> Adapters Redis (Fase 4)
    └── config              -> Beans de configuração
```

Cada pacote tem um `_README.md` temporário explicando a sua responsabilidade.

## Como rodar localmente

1. Subir a infraestrutura (SQL Server + Redis):
   ```bash
   docker compose up -d
   ```
2. Compilar e rodar os testes (requer Maven instalado — `mvn -v` para checar):
   ```bash
   mvn test
   ```
3. Subir a aplicação:
   ```bash
   mvn spring-boot:run
   ```

> Se preferir não instalar o Maven localmente, gere o wrapper com
> `mvn -N wrapper:wrapper` na raiz do projeto (ou use a IDE, que geralmente
> já traz Maven embutido) e passe a usar `./mvnw` no lugar de `mvn`.

> Nota: como está agora, o projeto sobe mas ainda não expõe nenhum endpoint —
> isso é esperado. A ideia é começar pela Fase 1 (domínio em Java puro,
> guiado por testes) antes de plugar Spring/JPA.

## Onde começar

Comece por `domain/model`, escrevendo primeiro o teste (em
`src/test/java/com/bankflow/domain/model`) e só depois a classe que o faz
passar. Sugestão de ordem: `Cliente` -> `Conta` -> `Transacao` ->
`LimiteDiario`.
