package com.sulwork.backend.cobertura;

import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.cobertura.dto.CoberturaResposta;
import com.sulwork.backend.cobertura.dto.CoberturaResposta.PorCategoria;
import com.sulwork.backend.cobertura.dto.CoberturaResposta.PorItem;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoberturaService {

    private final ContribuicaoRepository contribuicaoRepository;
    private final EventoService eventoService;
    private final CalculoCobertura calculo;

    @Transactional(readOnly = true)
    public CoberturaResposta calcular(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        var somasItens = contribuicaoRepository.somarPorItemDoEvento(eventoId);
        var somasCategorias = contribuicaoRepository.somarPorCategoriaDoEvento(eventoId);

        var itensCalculo = somasItens.stream().map(CoberturaService::paraCalculo).toList();
        var itens = somasItens.stream().map(this::porItem).toList();
        var categorias = somasCategorias.stream().map(this::porCategoria).toList();

        int obrigatorios = (int) itensCalculo.stream().filter(CalculoCobertura.Item::obrigatorio).count();
        int obrigatoriosCompletos = (int) itensCalculo.stream()
                .filter(i -> i.obrigatorio() && i.contribuido() >= i.necessario())
                .count();

        return new CoberturaResposta(eventoId, percentual(calculo.geral(itensCalculo)),
                obrigatoriosCompletos, obrigatorios, categorias, itens);
    }

    private PorItem porItem(SomaItem s) {
        long faltante = Math.max(0, s.getNecessario() - s.getContribuido());
        return new PorItem(s.getItemId(), s.getNome(), s.getCategoriaId(), UnidadeMedida.valueOf(s.getUnidade()),
                s.getObrigatorio(), s.getNecessario(), s.getContribuido(), faltante,
                percentual(calculo.porItem(paraCalculo(s))));
    }

    private PorCategoria porCategoria(SomaCategoria s) {
        var categoria = new CalculoCobertura.Categoria(s.getMeta(), s.getContribuido());
        return new PorCategoria(s.getCategoriaId(), s.getNome(), UnidadeMedida.valueOf(s.getUnidade()),
                s.getContribuido(), s.getMeta(), percentual(calculo.porCategoria(categoria)));
    }

    private static CalculoCobertura.Item paraCalculo(SomaItem s) {
        return new CalculoCobertura.Item(s.getNecessario(), s.getContribuido(), s.getObrigatorio());
    }

    // fração de 0 a 1 vira percentual com uma casa: 0.625 -> 62.5
    public static BigDecimal percentual(double fracao) {
        return BigDecimal.valueOf(fracao * 100).setScale(1, RoundingMode.HALF_UP);
    }
}
