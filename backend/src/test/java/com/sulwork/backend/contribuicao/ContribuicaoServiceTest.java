package com.sulwork.backend.contribuicao;

import static com.sulwork.backend.evento.EventosDeTeste.AGORA;
import static com.sulwork.backend.evento.EventosDeTeste.eventoEm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sulwork.backend.categoria.Categoria;
import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.dto.ContribuicaoRequisicao;
import com.sulwork.backend.evento.Evento;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.item.Item;
import com.sulwork.backend.item.ItemRepository;
import com.sulwork.backend.participacao.Participacao;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.Participante;
import com.sulwork.backend.participante.ParticipanteService;
import com.sulwork.backend.participante.RestricaoAlimentar;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContribuicaoServiceTest {

    static final LocalDateTime PRAZO = AGORA.plusDays(8);
    static final ContribuicaoRequisicao REQ = new ContribuicaoRequisicao(2L, 9L, 3);

    @Mock
    ContribuicaoRepository repository;
    @Mock
    ParticipacaoRepository participacaoRepository;
    @Mock
    ItemRepository itemRepository;
    @Mock
    EventoService eventoService;
    @Mock
    ParticipanteService participanteService;

    Evento evento;
    Participacao participacao;
    Item item;

    @BeforeEach
    void configurar() {
        evento = eventoEm(StatusEvento.ABERTO);
        participacao = new Participacao(evento, new Participante("Ana", "52998224725", Set.of(RestricaoAlimentar.SEM_LACTOSE), null));
        ReflectionTestUtils.setField(participacao, "id", 5L);
        item = new Item(new Categoria(evento, "Bebidas", 20, UnidadeMedida.LITRO), "Suco", 10, true, Set.of());
        ReflectionTestUtils.setField(item, "id", 9L);
    }

    ContribuicaoService serviceEm(LocalDateTime agora) {
        var relogio = Clock.fixed(agora.atZone(RelogioConfig.FUSO).toInstant(), RelogioConfig.FUSO);
        return new ContribuicaoService(repository, participacaoRepository, itemRepository, eventoService,
                participanteService, relogio);
    }

    void prepararCriacao(long jaContribuido) {
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(participacaoRepository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(participacao));
        when(itemRepository.findByIdAndEventoComLock(9L, 1L)).thenReturn(Optional.of(item));
        when(repository.somarAtivasDoItem(9L)).thenReturn(jaContribuido);
    }

    @Test
    void criaQuandoCabeExatamenteNoLimite() {
        prepararCriacao(7);
        when(repository.save(any(Contribuicao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = serviceEm(AGORA).criar(1L, REQ);

        assertThat(resposta.quantidade()).isEqualTo(3);
        assertThat(resposta.unidade()).isEqualTo(UnidadeMedida.LITRO);
        assertThat(resposta.avisos()).isEmpty();
    }

    @Test
    void somaPassandoDoLimiteEhBloqueada() {
        prepararCriacao(8);

        assertThatThrownBy(() -> serviceEm(AGORA).criar(1L, REQ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A quantidade ultrapassa o necessário para o item Suco. Faltam 2.");
        verify(repository, never()).save(any());
    }

    @Test
    void foraDoPrazoEhBloqueado() {
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);

        assertThatThrownBy(() -> serviceEm(PRAZO).criar(1L, REQ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O prazo de alterações do evento já venceu.");
        verifyNoInteractions(itemRepository);
    }

    @ParameterizedTest
    @EnumSource(value = StatusEvento.class, names = "ABERTO", mode = EnumSource.Mode.EXCLUDE)
    void eventoNaoAbertoEhBloqueado(StatusEvento status) {
        when(eventoService.buscarEntidade(1L)).thenReturn(eventoEm(status));

        assertThatThrownBy(() -> serviceEm(AGORA).criar(1L, REQ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("precisa estar ABERTO");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void participacaoInativaEhBloqueada() {
        participacao.sair();
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(participacaoRepository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(participacao));

        assertThatThrownBy(() -> serviceEm(AGORA).criar(1L, REQ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Só quem participa do evento pode contribuir.");
        verify(repository, never()).save(any());
    }

    @Test
    void semParticipacaoEhBloqueado() {
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(participacaoRepository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceEm(AGORA).criar(1L, REQ)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void segundaContribuicaoAtivaNoMesmoItemEhConflito() {
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(participacaoRepository.findByEventoIdAndParticipanteId(1L, 2L)).thenReturn(Optional.of(participacao));
        when(itemRepository.findByIdAndEventoComLock(9L, 1L)).thenReturn(Optional.of(item));
        when(repository.existsByParticipacaoIdAndItemIdAndAtivoTrue(5L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> serviceEm(AGORA).criar(1L, REQ)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void avisaQuandoItemConflitaComRestricaoDoParticipanteSemBloquear() {
        item.atualizar(item.getCategoria(), "Vitamina", 10, true, Set.of(Alergeno.LACTOSE), 0);
        prepararCriacao(0);
        when(repository.save(any(Contribuicao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = serviceEm(AGORA).criar(1L, REQ);

        assertThat(resposta.avisos()).containsExactly(
                "O item Vitamina contém LACTOSE, que conflita com sua restrição SEM_LACTOSE.");
        verify(repository).save(any(Contribuicao.class));
    }

    @Test
    void alterarSomaAsOutrasContribuicoesEANovaQuantidade() {
        var contribuicao = new Contribuicao(participacao, item, 2);
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(repository.findAtivaNoEvento(30L, 1L)).thenReturn(Optional.of(contribuicao));
        when(itemRepository.findByIdAndEventoComLock(9L, 1L)).thenReturn(Optional.of(item));
        when(repository.somarOutrasAtivasDoItem(9L, 30L)).thenReturn(7L);

        assertThatThrownBy(() -> serviceEm(AGORA).alterar(1L, 30L, 4))
                .hasMessage("A quantidade ultrapassa o necessário para o item Suco. Faltam 3.");
        assertThat(contribuicao.getQuantidade()).isEqualTo(2);

        serviceEm(AGORA).alterar(1L, 30L, 3);
        assertThat(contribuicao.getQuantidade()).isEqualTo(3);
    }

    @Test
    void removerEhExclusaoLogica() {
        var contribuicao = new Contribuicao(participacao, item, 2);
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);
        when(repository.findAtivaNoEvento(30L, 1L)).thenReturn(Optional.of(contribuicao));

        serviceEm(AGORA).remover(1L, 30L);

        assertThat(contribuicao.isAtivo()).isFalse();
        verify(repository, never()).delete(any());
    }

    @Test
    void removerForaDoPrazoEhBloqueado() {
        when(eventoService.buscarEntidade(1L)).thenReturn(evento);

        assertThatThrownBy(() -> serviceEm(PRAZO.plusHours(1)).remover(1L, 30L))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).findAtivaNoEvento(any(), any());
    }
}
