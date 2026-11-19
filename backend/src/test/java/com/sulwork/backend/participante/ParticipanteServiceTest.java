package com.sulwork.backend.participante;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.dto.ParticipanteEdicaoRequisicao;
import com.sulwork.backend.participante.dto.ParticipanteRequisicao;
import java.util.List;
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
    @Mock
    ParticipacaoRepository participacaoRepository;

    ParticipanteService service;

    @BeforeEach
    void configurar() {
        service = new ParticipanteService(repository, participacaoRepository);
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

        assertThat(resposta.cpf()).isEqualTo("***.982.247-**");
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
    void criarComCpfDeParticipanteExcluidoReativaComOsDadosNovos() {
        var inativo = participante(1, "52998224725");
        inativo.desativar();
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(inativo));

        var resposta = service.criar(new ParticipanteRequisicao("Bia", "529.982.247-25", Set.of(RestricaoAlimentar.SEM_MEL), null));

        assertThat(inativo.isAtivo()).isTrue();
        assertThat(resposta.id()).isEqualTo(1L);
        assertThat(resposta.nome()).isEqualTo("Bia");
        assertThat(resposta.restricoes()).containsExactly(RestricaoAlimentar.SEM_MEL);
        verify(repository, never()).save(any());
    }

    @Test
    void editarMantendoOProprioCpfNaoEhConflito() {
        var ana = participante(1, "52998224725");
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ana));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(ana));

        var resposta = service.editar(1L, new ParticipanteEdicaoRequisicao("Ana Maria", "52998224725", Set.of(), null));

        assertThat(resposta.nome()).isEqualTo("Ana Maria");
        assertThat(resposta.restricoes()).isEmpty();
    }

    @Test
    void editarParaCpfDeOutroParticipanteRetornaConflito() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(participante(1, "52998224725")));
        when(repository.findByCpf("11144477735")).thenReturn(Optional.of(participante(2, "11144477735")));

        assertThatThrownBy(() -> service.editar(1L, new ParticipanteEdicaoRequisicao("Ana", "11144477735", null, null)))
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
    void editarSemCpfMantemOAtual() {
        var ana = participante(1, "52998224725");
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ana));
        when(repository.findByCpf("52998224725")).thenReturn(Optional.of(ana));

        service.editar(1L, new ParticipanteEdicaoRequisicao("Ana Paula", null, null, null));

        assertThat(ana.getCpf()).isEqualTo("52998224725");
        assertThat(ana.getNome()).isEqualTo("Ana Paula");
    }

    @Test
    void excluirQuemEstaEmEventoEmAndamentoEhBloqueado() {
        var ana = participante(1, "52998224725");
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ana));
        when(participacaoRepository.findNomesDosEventos(any(), any())).thenReturn(List.of("Café de fim de ano"));

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O participante está no evento \"Café de fim de ano\", que ainda não terminou. "
                        + "Tire-o do evento antes de excluir.");
        assertThat(ana.isAtivo()).isTrue();
    }

    @Test
    void buscarInativoOuInexistenteRetornaNaoEncontrado() {
        when(repository.findByIdAndAtivoTrue(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(9L))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Participante 9 não encontrado.");
    }
}
