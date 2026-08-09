# BankFlow

Projeto de estudo — núcleo de conta digital (Core Banking simplificado) em
Java 21 + Spring Boot 3, arquitetura hexagonal, SQL Server e Redis.

Consulte `proposta-projeto-bankflow.md` para a
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
