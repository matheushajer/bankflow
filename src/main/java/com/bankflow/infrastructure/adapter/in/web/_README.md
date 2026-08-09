# infrastructure/adapter/in/web

Controllers REST (ex: `ContaController`, `TransacaoController`) e DTOs de
request/response. Chamam as portas de entrada (`application/port/in`) — não
conhecem entidades JPA nem o domínio diretamente, só os DTOs.
