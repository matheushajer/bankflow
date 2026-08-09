# application/port/out

Interfaces que o domínio/aplicação precisa que o mundo externo implemente
(ex: `ContaRepository`, `TransacaoRepository`, `IdempotenciaCache`).
Os adapters de saída (JPA, Redis) implementam essas interfaces em
`infrastructure/adapter/out`.
