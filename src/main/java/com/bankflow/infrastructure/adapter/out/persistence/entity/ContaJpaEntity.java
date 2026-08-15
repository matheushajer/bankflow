package com.bankflow.infrastructure.adapter.out.persistence.entity;

import com.bankflow.domain.model.enums.StatusConta;
import com.bankflow.domain.model.enums.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Espelho da tabela {@code conta} (ver V2__create_conta.sql). Sem nenhuma
 * regra de negocio - so estrutura de persistencia. A entidade de dominio
 * {@code Conta} (domain.model) nunca deve importar esta classe; a conversao
 * entre as duas fica no adapter ({@code ContaRepositoryJpaAdapter}).
 * <p>
 * Decisao de arquitetura: {@code clienteId} e uma coluna UUID simples, NUNCA
 * {@code @ManyToOne} para {@link ClienteJpaEntity}. Isso evita acoplar os
 * agregados Conta e Cliente (join implicito, lazy loading, cascade
 * acidental) e facilita uma futura decomposicao em microsservicos.
 */
@Entity
@Table(name = "conta")
public class ContaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(nullable = false, length = 10)
    private String agencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConta tipo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal saldo;

    @Column(name = "limite_cheque_especial", nullable = false, precision = 18, scale = 2)
    private BigDecimal limiteChequeEspecial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConta status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    /**
     * Lock otimista (protege saldo/limiteChequeEspecial contra depositos e
     * saques concorrentes na mesma conta). Populado a partir de
     * {@code Conta.getVersion()} - o dominio carrega esse valor de forma
     * opaca (nao participa de nenhuma regra de negocio) desde a leitura
     * (ver {@link com.bankflow.domain.model.Conta#restaurar}) ate o
     * proximo save. Ao salvar, {@code ContaRepositoryJpaAdapter} sempre
     * constroi uma entidade nova carregando essa versao "antiga" e manda
     * pro {@code save()} -> o Hibernate faz o merge comparando com a
     * versao atual do banco: se alguem alterou a linha nesse meio tempo,
     * lanca {@code ObjectOptimisticLockingFailureException} em vez de
     * sobrescrever silenciosamente. Por isso NAO existe setter aqui - o
     * unico jeito de "definir" a versao e-la vir junto no construtor,
     * refletindo o que foi lido do banco (ou 0, para conta nova).
     */
    @Version
    @Column(nullable = false)
    private Long version;

    protected ContaJpaEntity() {
        // construtor exigido pelo JPA - nao usar diretamente
    }

    public ContaJpaEntity(UUID id, UUID clienteId, String numero, String agencia, TipoConta tipo, BigDecimal saldo,
                           BigDecimal limiteChequeEspecial, StatusConta status, LocalDateTime dataAbertura, Long version) {
        this.id = id;
        this.clienteId = clienteId;
        this.numero = numero;
        this.agencia = agencia;
        this.tipo = tipo;
        this.saldo = saldo;
        this.limiteChequeEspecial = limiteChequeEspecial;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public String getNumero() {
        return numero;
    }

    public String getAgencia() {
        return agencia;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getLimiteChequeEspecial() {
        return limiteChequeEspecial;
    }

    public StatusConta getStatus() {
        return status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public Long getVersion() {
        return version;
    }

}
