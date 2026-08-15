package com.bankflow.infrastructure.adapter.out.persistence.entity;

import com.bankflow.domain.model.enums.StatusCliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Espelho da tabela {@code cliente} (ver V1__create_cliente.sql). Sem
 * nenhuma regra de negocio - so estrutura de persistencia. A entidade de
 * dominio {@code Cliente} (domain.model) nunca deve importar esta classe;
 * a conversao entre as duas fica no adapter ({@code ClienteRepositoryJpaAdapter}).
 */
@Entity
@Table(name = "cliente")
public class ClienteJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCliente status;

    protected ClienteJpaEntity() {
        // construtor exigido pelo JPA - nao usar diretamente
    }

    public ClienteJpaEntity(UUID id, String nome, String cpf, LocalDate dataNascimento, String email, StatusCliente status) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getEmail() {
        return email;
    }

    public StatusCliente getStatus() {
        return status;
    }

}
