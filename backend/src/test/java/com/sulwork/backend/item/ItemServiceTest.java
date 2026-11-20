package com.sulwork.backend.item;

import static com.sulwork.backend.evento.EventosDeTeste.eventoEm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.categoria.Categoria;
import com.sulwork.backend.categoria.CategoriaService;
import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.item.dto.ItemRequisicao;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    static final ItemRequisicao REQ = new ItemRequisicao("Pão de queijo", 3L, 20, true, Set.of(Alergeno.LACTOSE));

    @Mock
    ItemRepository repository;
    @Mock
    CategoriaService categoriaService;
    @Mock
    EventoService eventoService;
    @Mock
    ContribuicaoRepository contribuicaoRepository;

    ItemService service;

    @BeforeEach
    void configurar() {
        service = new ItemService(repository, categoriaService, eventoService, contribuicaoRepository);
    }

    @ParameterizedTest
    @EnumSource(value = StatusEvento.class, names = {"RASCUNHO", "ABERTO"})
    void criaEEditaEmRascunhoEAberto(StatusEvento status) {
        var evento = eventoEm(status);
        var salgados = new Categoria(evento, "Salgados", 50, UnidadeMedida.UNIDADE);
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(categoriaService.buscarEntidade(1L, 3L)).thenReturn(salgados);
        when(repository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByIdAndEventoComLock(9L, 1L)).thenReturn(Optional.of(new Item(salgados, "Coxinha", 10, false, Set.of())));

        var criado = service.criar(1L, REQ);
        var editado = service.editar(1L, 9L, REQ);

        assertThat(criado.unidade()).isEqualTo(UnidadeMedida.UNIDADE);
        assertThat(criado.alergenos()).containsExactly(Alergeno.LACTOSE);
        assertThat(editado.nome()).isEqualTo("Pão de queijo");
    }

    @ParameterizedTest
    @EnumSource(value = StatusEvento.class, names = {"ENCERRADO", "REALIZADO", "CANCELADO"})
    void bloqueiaCriarEditarEExcluirNosDemaisStatus(StatusEvento status) {
        when(eventoService.travarParaAlteracao(1L)).thenReturn(eventoEm(status));

        assertThatThrownBy(() -> service.criar(1L, REQ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("só pode ser editado com status RASCUNHO ou ABERTO");
        assertThatThrownBy(() -> service.editar(1L, 9L, REQ)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.excluir(1L, 9L)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void categoriaDeOutroEventoImpedeCriar() {
        when(eventoService.travarParaAlteracao(1L)).thenReturn(eventoEm(StatusEvento.RASCUNHO));
        when(categoriaService.buscarEntidade(1L, 3L)).thenThrow(new NaoEncontradoException("Categoria 3 não encontrada neste evento."));

        assertThatThrownBy(() -> service.criar(1L, REQ)).isInstanceOf(NaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void excluiItemSemContribuicoes() {
        var evento = eventoEm(StatusEvento.ABERTO);
        var item = new Item(new Categoria(evento, "Salgados", 50, UnidadeMedida.UNIDADE), "Coxinha", 10, false, Set.of());
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(repository.findByIdAndEventoComLock(9L, 1L)).thenReturn(Optional.of(item));

        service.excluir(1L, 9L);

        verify(repository).delete(item);
    }
}
