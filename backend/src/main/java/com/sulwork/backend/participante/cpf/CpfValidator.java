package com.sulwork.backend.participante.cpf;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CpfValidator implements ConstraintValidator<CPF, String> {

    private static final Pattern FORMATO = Pattern.compile("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

    // null fica a cargo do @NotBlank
    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext contexto) {
        return cpf == null || valido(cpf);
    }

    public static String somenteNumeros(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        if (cpf == null || !FORMATO.matcher(cpf).matches()) {
            return false;
        }
        String numeros = somenteNumeros(cpf);
        // todos iguais (111.111.111-11...) passam na conta, mas não são CPFs reais
        if (numeros.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(numeros, 9) == numeros.charAt(9) - '0'
                && digitoVerificador(numeros, 10) == numeros.charAt(10) - '0';
    }

    // pesos de (tamanho + 1) até 2; resto < 2 vira 0, senão 11 - resto
    private static int digitoVerificador(String numeros, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (numeros.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
