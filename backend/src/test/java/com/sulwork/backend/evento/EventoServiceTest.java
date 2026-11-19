package com.sulwork.backend.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.evento.dto.EventoRequisicao;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 12, 1, 10, 0);

    @Mock
    EventoRepository repository;

    EventoService service;

    @BeforeEach
    void configurar() {
        var relogioFixo = Clock.fixed(AGORA.atZone(RelogioConfig.FUSO).toInstant(), RelogioConfig.FUSO);
        service = new EventoService(repository, relogioFixo);
    }

    static Evento eventoEmRascunho() {
        return new Evento("Café", "desc", TipoEvento.CAFE_DA_MANHA, AGORA.plusDays(10), AGORA.plusDays(8), AGORA);
    }

    @Test
    void criarSalvaEmRascunhoUsandoORelogio() {
        when(repository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));
        var req = new EventoRequisicao("Café", "desc", TipoEvento.CAFE_DA_MANHA, AGORA.plusDays(10), AGORA.plusDays(8));

        var resposta = service.criar(req);

        assertThat(resposta.status()).isEqualTo(StatusEvento.RASCUNHO);
        assertThat(resposta.proximosStatus()).containsExactly(StatusEvento.ABERTO, StatusEvento.CANCELADO);
    }

    @Test
    void criarComDataNoPassadoNaoSalva() {
        var req = new EventoRequisicao("Café", null, TipoEvento.LANCHE, AGORA.minusDays(1), AGORA.minusDays(2));

        assertThatThrownBy(() -> service.criar(req)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void listarSemFiltroBuscaTodos() {
        when(repository.findAllByOrderByDataHoraAsc()).thenReturn(List.of(eventoEmRascunho()));

        assertThat(service.listar(null)).hasSize(1);
        verify(repository, never()).findByStatusOrderByDataHoraAsc(any());
    }

    @Test
    void listarComFiltroBuscaPorStatus() {
        when(repository.findByStatusOrderByDataHoraAsc(StatusEvento.ABERTO)).thenReturn(List.of());

        assertThat(service.listar(StatusEvento.ABERTO)).isEmpty();
        verify(repository, never()).findAllByOrderByDataHoraAsc();
    }

    @Test
    void buscarInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Evento 99 não encontrado.");
    }

    @Test
    void excluirEmRascunhoRemove() {
        var evento = eventoEmRascunho();
        when(repository.findById(1L)).thenReturn(Optional.of(evento));

        service.excluir(1L);

        verify(repository).delete(evento);
    }

    @Test
    void excluirForaDeRascunhoNaoRemove() {
        var evento = eventoEmRascunho();
        evento.mudarStatus(StatusEvento.ABERTO, AGORA);
        when(repository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void mudarStatusAlteraAEntidadeSemChamarSave() {
        var evento = eventoEmRascunho();
        when(repository.findById(1L)).thenReturn(Optional.of(evento));

        var resposta = service.mudarStatus(1L, StatusEvento.ABERTO);

        assertThat(resposta.status()).isEqualTo(StatusEvento.ABERTO);
        verify(repository, never()).save(any());
    }

    @Test
    void editarAplicaNovosDados() {
        when(repository.findById(1L)).thenReturn(Optional.of(eventoEmRascunho()));
        var req = new EventoRequisicao("Almoço", null, TipoEvento.ALMOCO, AGORA.plusDays(20), AGORA.plusDays(15));

        var resposta = service.editar(1L, req);

        assertThat(resposta.nome()).isEqualTo("Almoço");
        assertThat(resposta.dataHora()).isEqualTo(AGORA.plusDays(20));
    }
}
