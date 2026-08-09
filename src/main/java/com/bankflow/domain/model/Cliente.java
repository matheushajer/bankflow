package com.bankflow.domain.model;

import com.bankflow.domain.exception.ClienteMenorDeIdadeException;
import com.bankflow.domain.exception.CpfInvalidoException;
import com.bankflow.domain.model.enums.StatusCliente;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Entidade de dominio Cliente. Java puro (sem anotacoes de framework) - as
 * regras de negocio ficam aqui, nao na camada de aplicacao ou infraestrutura.
 * <p>
 * Padrao adotado no projeto:
 * <ul>
 *     <li>construtor privado + metodo estatico de fabrica ({@code criar}),
 *     garantindo que nunca existe instancia em estado invalido;</li>
 *     <li>violacao de regra de negocio lanca excecao de dominio especifica
 *     (pacote {@code domain.exception});</li>
 *     <li>dado obrigatorio ausente ou mal formatado lanca
 *     {@link IllegalArgumentException} (nao e regra de negocio, e validacao
 *     de entrada).</li>
 * </ul>
 */
public class Cliente {

    private static final int IDADE_MINIMA = 18;

    private final UUID id;
    private final String nome;
    private final String cpf;
    private final LocalDate dataNascimento;
    private final String email;
    private StatusCliente status;

    private Cliente(UUID id, String nome, String cpf, LocalDate dataNascimento, String email, StatusCliente status) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.status = status;
    }

    public static Cliente criar(String nome, String cpf, LocalDate dataNascimento, String email) {
        validarNome(nome);
        validarEmail(email);
        validarCpf(cpf);
        validarMaioridade(dataNascimento);

        String cpfSomenteNumeros = cpf.replaceAll("\\D", "");
        return new Cliente(UUID.randomUUID(), nome.trim(), cpfSomenteNumeros, dataNascimento, email.trim(), StatusCliente.ATIVO);
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio");
        }
    }

    private static void validarEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email invalido: " + email);
        }
    }

    private static void validarCpf(String cpf) {
        if (cpf == null || !cpfValido(cpf)) {
            throw new CpfInvalidoException("CPF invalido: " + cpf);
        }
    }

    private static void validarMaioridade(LocalDate dataNascimento) {
        if (dataNascimento == null || Period.between(dataNascimento, LocalDate.now()).getYears() < IDADE_MINIMA) {
            throw new ClienteMenorDeIdadeException("Cliente deve ser maior de " + IDADE_MINIMA + " anos");
        }
    }

    private static boolean cpfValido(String cpf) {
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        int[] numeros = digitos.chars().map(c -> c - '0').toArray();
        int digitoVerificador1 = calcularDigitoVerificador(numeros, 9, 10);
        int digitoVerificador2 = calcularDigitoVerificador(numeros, 10, 11);
        return numeros[9] == digitoVerificador1 && numeros[10] == digitoVerificador2;
    }

    private static int calcularDigitoVerificador(int[] numeros, int quantidadeDigitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < quantidadeDigitos; i++) {
            soma += numeros[i] * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public void bloquear() {
        this.status = StatusCliente.BLOQUEADO;
    }

    public void ativar() {
        this.status = StatusCliente.ATIVO;
    }

    public void inativar() {
        this.status = StatusCliente.INATIVO;
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
