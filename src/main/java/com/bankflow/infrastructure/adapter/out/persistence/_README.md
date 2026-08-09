# infrastructure/adapter/out/persistence

Implementações JPA das portas de saída (ex: `ContaRepositoryJpaAdapter`),
usando as entidades de `entity/` (anotadas com `@Entity`) e o
`ContaSpringDataRepository` (interface `JpaRepository`). Responsável por
converter entre entidade JPA e entidade de domínio.
