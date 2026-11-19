package com.sulwork.backend.participante.cpf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    final CpfValidator validador = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735", "12345678909", "39053344705"})
    void aceitaCpfsValidosSemMascara(String cpf) {
        assertThat(validador.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "111.444.777-35", "123.456.789-09"})
    void aceitaCpfsValidosComMascara(String cpf) {
        assertThat(validador.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "52998224735", "12345678900", "529.982.247-26"})
    void rejeitaDigitosVerificadoresErrados(String cpf) {
        assertThat(validador.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "22222222222", "33333333333", "44444444444",
            "55555555555", "66666666666", "77777777777", "88888888888", "99999999999", "111.111.111-11"})
    void rejeitaTodosOsDigitosIguais(String cpf) {
        assertThat(validador.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "5299822472", "529982247250", "1234"})
    void rejeitaTamanhoErrado(String cpf) {
        assertThat(validador.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.24725", "529982247-25", "529 982 247 25", "529-982-247.25", "52998224a25"})
    void rejeitaMascaraMalformadaOuLetras(String cpf) {
        assertThat(validador.isValid(cpf, null)).isFalse();
    }

    @Test
    void nullEhValidoPorqueAObrigatoriedadeEhDoNotBlank() {
        assertThat(validador.isValid(null, null)).isTrue();
    }

    @Test
    void somenteNumerosTiraAMascara() {
        assertThat(CpfValidator.somenteNumeros("529.982.247-25")).isEqualTo("52998224725");
    }
}
