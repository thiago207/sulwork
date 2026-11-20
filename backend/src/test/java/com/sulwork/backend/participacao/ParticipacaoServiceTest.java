package com.sulwork.backend.participacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.Evento;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.evento.TipoEvento;
import com.sulwork.backend.participante.Participante;
import com.sulwork.backend.participante.ParticipanteService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ParticipacaoServiceTest {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 12, 1, 10, 0);
    static final LocalDateTime PRAZO = AGORA.plusDays(8);

    @Mock
    ParticipacaoRepository repository;
    @Mock
    EventoService eventoService;
    @Mock
    ParticipanteService participanteService;
    @Mock
    ContribuicaoRepository contribuicaoRepository;

    Evento evento;
    Participante participante;

    @BeforeEach
    void configurar() {
        evento = new Evento("Café", null, TipoEvento.CAFE_DA_MANHA, AGORA.plusDays(10), PRAZO, AGORA);
        participante = new Participante("Ana", "52998224725", Set.of(), null);
    }

    ParticipacaoService serviceEm(LocalDateTime agora) {
        var relogio = Clock.fixed(agora.atZone(RelogioConfig.FUSO).toInstant(), RelogioConfig.FUSO);
        return new ParticipacaoService(repository, eventoService, participanteService, contribuicaoRepository, relogio);
    }

    void eventoAberto() {
        evento.mudarStatus(StatusEvento.ABERTO, AGORA);
    }

    void prepararBuscas() {
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(participanteService.travarAtivo(2L)).thenReturn(participante);
    }

    @Test
    void entrarPelaPrimeiraVezCriaParticipacao() {
        eventoAberto();
        prepararBuscas();
        when(repository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.empty());
        when(repository.save(any(Participacao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = serviceEm(AGORA).entrar(1L, 2L);

        assertThat(resposta.ativo()).isTrue();
        verify(repository).save(any(Participacao.class));
    }

    @Test
    void entrarDeNovoDepoisDeSairReativaAMesmaParticipacao() {
        eventoAberto();
        prepararBuscas();
        var antiga = new Participacao(evento, participante);
        antiga.sair();
        when(repository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(antiga));

        serviceEm(AGORA).entrar(1L, 2L);

        assertThat(antiga.isAtivo()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void entrarJaEstandoNoEventoRetornaConflito() {
        eventoAberto();
        prepararBuscas();
        when(repository.findByEventoIdAndParticipanteId(1L, 2L))
                .thenReturn(Optional.of(new Participacao(evento, participante)));

        assertThatThrownBy(() -> serviceEm(AGORA).entrar(1L, 2L))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("O participante já está neste evento.");
    }

    @Test
    void entrarEmEventoRascunhoEhBloqueado() {
        prepararBuscas();

        assertThatThrownBy(() -> serviceEm(AGORA).entrar(1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("precisa estar ABERTO");
        verify(repository, never()).save(any());
    }

    @Test
    void entrarComPrazoVencidoEhBloqueado() {
        eventoAberto();
        prepararBuscas();

        assertThatThrownBy(() -> serviceEm(PRAZO.plusMinutes(1)).entrar(1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O prazo de alterações do evento já venceu.");
    }

    @Test
    void sairDesativaAParticipacaoEAsContribuicoes() {
        eventoAberto();
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        var participacao = new Participacao(evento, participante);
        ReflectionTestUtils.setField(participacao, "id", 5L);
        when(repository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(participacao));

        serviceEm(AGORA).sair(1L, 2L);

        assertThat(participacao.isAtivo()).isFalse();
        verify(contribuicaoRepository).desativarDaParticipacao(5L);
        verify(repository, never()).delete(any());
    }

    @Test
    void sairSemEstarNoEventoRetornaNaoEncontrado() {
        eventoAberto();
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        var jaSaiu = new Participacao(evento, participante);
        jaSaiu.sair();
        when(repository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(jaSaiu));

        assertThatThrownBy(() -> serviceEm(AGORA).sair(1L, 2L))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("O participante não está neste evento.");
    }

    @Test
    void sairComEventoEncerradoEhBloqueado() {
        eventoAberto();
        evento.mudarStatus(StatusEvento.ENCERRADO, AGORA);
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);

        assertThatThrownBy(() -> serviceEm(AGORA).sair(1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("precisa estar ABERTO");
    }

    @Test
    void sairComPrazoVencidoEhBloqueado() {
        eventoAberto();
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);

        assertThatThrownBy(() -> serviceEm(PRAZO).sair(1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O prazo de alterações do evento já venceu.");
    }
}
