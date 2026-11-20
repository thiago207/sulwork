package com.sulwork.backend.categoria;

import static com.sulwork.backend.evento.EventosDeTeste.eventoEm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sulwork.backend.categoria.dto.CategoriaRequisicao;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.item.ItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    static final CategoriaRequisicao REQ = new CategoriaRequisicao("Bebidas", 10, UnidadeMedida.LITRO);

    @Mock
    CategoriaRepository repository;
    @Mock
    ItemRepository itemRepository;
    @Mock
    EventoService eventoService;
    @Mock
    ContribuicaoRepository contribuicaoRepository;

    CategoriaService service;

    @BeforeEach
    void configurar() {
        service = new CategoriaService(repository, itemRepository, eventoService, contribuicaoRepository);
    }

    @ParameterizedTest
    @EnumSource(value = StatusEvento.class, names = {"RASCUNHO", "ABERTO"})
    void criaEEditaEmRascunhoEAberto(StatusEvento status) {
        var evento = eventoEm(status);
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(repository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));
        var existente = new Categoria(evento, "Doces", 5, UnidadeMedida.UNIDADE);
        when(repository.findByIdAndEventoId(7L, 1L)).thenReturn(Optional.of(existente));

        assertThat(service.criar(1L, REQ).unidade()).isEqualTo(UnidadeMedida.LITRO);
        assertThat(service.editar(1L, 7L, REQ).nome()).isEqualTo("Bebidas");
    }

    @ParameterizedTest
    @EnumSource(value = StatusEvento.class, names = {"ENCERRADO", "REALIZADO", "CANCELADO"})
    void bloqueiaCriarEditarEExcluirNosDemaisStatus(StatusEvento status) {
        when(eventoService.travarParaAlteracao(1L)).thenReturn(eventoEm(status));

        assertThatThrownBy(() -> service.criar(1L, REQ)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.editar(1L, 7L, REQ)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.excluir(1L, 7L)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void naoExcluiCategoriaComItens() {
        var evento = eventoEm(StatusEvento.RASCUNHO);
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(repository.findByIdAndEventoId(7L, 1L)).thenReturn(Optional.of(new Categoria(evento, "Doces", 5, UnidadeMedida.UNIDADE)));
        when(itemRepository.existsByCategoriaId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L, 7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Remova os itens da categoria antes de excluí-la.");
        verify(repository, never()).delete(any());
    }

    @Test
    void excluiCategoriaSemItens() {
        var evento = eventoEm(StatusEvento.ABERTO);
        var categoria = new Categoria(evento, "Doces", 5, UnidadeMedida.UNIDADE);
        when(eventoService.travarParaAlteracao(1L)).thenReturn(evento);
        when(repository.findByIdAndEventoId(7L, 1L)).thenReturn(Optional.of(categoria));

        service.excluir(1L, 7L);

        verify(repository).delete(categoria);
    }

    @Test
    void categoriaDeOutroEventoNaoEhEncontrada() {
        when(repository.findByIdAndEventoId(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(2L, 7L))
                .isInstanceOf(NaoEncontradoException.class)
                .hasMessage("Categoria 7 não encontrada neste evento.");
    }
}
