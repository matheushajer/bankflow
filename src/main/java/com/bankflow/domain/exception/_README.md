# domain/exception

Exceções de negócio (ex: `SaldoInsuficienteException`, `LimiteDiarioExcedidoException`,
`ContaBloqueadaException`). Devem estender `RuntimeException` e ser lançadas pelas
próprias entidades/regras — não pela camada web.
