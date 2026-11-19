package com.sulwork.backend.participante;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.participante.dto.ParticipanteRequisicao;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ParticipanteServiceTest {

    @Mock
    ParticipanteRepository repository;

    ParticipanteService service;

    @BeforeEach
    void configurar() {
        service = new ParticipanteService(repository);
    }

    static Participante participante(long id, String cpf) {
        var p = new Participante("Ana", cpf, Set.of(RestricaoAlimentar.VEGANO), null);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void criarSalvaCpfSomenteComNumeros() {
        when(repository.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(repository.save(any(Participante.class))).thenAnswer(inv -> inv.getArgument(0));
        var req = new ParticipanteRequisicao("Ana", "529.982.247-25", Set.of(RestricaoAlimentar.SEM_GLUTEN), "celíaca");

        var resposta = service.criar(req);

        assertThat(resposta.cpf()).isEqualTo("52998224725");
        assertThat(resposta.restricoes()).containsExactly(RestricaoAlimentar.SEM_GLUTEN);
        assertThat(resposta.observacaoRestricao()).isEqualTo("celíaca");
    }

    @Test
    void criarSemRestricoesDevolveListaVazia() {
        when(repository.findByCpf(any())).thenReturn(Optional.empty());
        when(repository.save(any(Participante.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.criar(new ParticipanteRequisicao("Ana", "52998224725", null, null));

        assertThat(resposta.restricoes()).isEmpty();
    }

    @Test
    void criarComCpfDeParticipanteAtivoRetornaConflito() {
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(participante(1, "52998224725")));

        assertThatThrownBy(() -> service.criar(new ParticipanteRequisicao("Bia", "52998224725", null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um participante com este CPF.");
        verify(repository, never()).save(any());
    }

    @Test
    void criarComCpfDeParticipanteInativoTambemRetornaConflito() {
        var inativo = participante(1, "52998224725");
        inativo.desativar();
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.criar(new ParticipanteRequisicao("Bia", "529.982.247-25", null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um participante inativo com este CPF.");
    }

    @Test
    void editarMantendoOProprioCpfNaoEhConflito() {
        var ana = participante(1, "52998224725");
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ana));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(ana));

        var resposta = service.editar(1L, new ParticipanteRequisicao("Ana Maria", "52998224725", Set.of(), null));

        assertThat(resposta.nome()).isEqualTo("Ana Maria");
        assertThat(resposta.restricoes()).isEmpty();
    }

    @Test
    void editarParaCpfDeOutroParticipanteRetornaConflito() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(participante(1, "52998224725")));
        when(repository.findByCpf("11144477735")).thenReturn(Optional.of(participante(2, "11144477735")));

        assertThatThrownBy(() -> service.editar(1L, new ParticipanteRequisicao("Ana", "11144477735", null, null)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void excluirDesativaSemApagar() {
        var ana = participante(1, "52998224725");
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ana));

        service.excluir(1L);

        assertThat(ana.isAtivo()).isFalse();
        verify(repository, never()).delete(any());
    }

    @Test
    void buscarInativoOuInexistenteRetornaNaoEncontrado() {
        when(repository.findByIdAndAtivoTrue(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(9L))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Participante 9 não encontrado.");
    }
}
