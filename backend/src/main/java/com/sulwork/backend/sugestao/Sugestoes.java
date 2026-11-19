package com.sulwork.backend.sugestao;

import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.cobertura.CalculoCobertura;
import com.sulwork.backend.cobertura.CoberturaService;
import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.participante.RestricaoAlimentar;
import com.sulwork.backend.sugestao.dto.SugestoesResposta.Conflitos;
import com.sulwork.backend.sugestao.dto.SugestoesResposta.Sugestao;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// regra 12, sem Spring nem banco: recebe os dados prontos e devolve a lista filtrada e ordenada
public final class Sugestoes {

    private Sugestoes() {
    }

    public record Candidato(Long itemId, String nome, String categoria, UnidadeMedida unidade, boolean obrigatorio,
                            int necessario, long contribuido, Set<Alergeno> alergenos, int suaContribuicao) {
    }

    public record Resultado(List<Sugestao> sugestoes, int ocultosPorConflito) {
    }

    private record Avaliado(Candidato item, double cobertura, long faltante, Conflitos conflitos) {
    }

    // obrigatórios primeiro, depois menor cobertura, maior faltante e nome (Collator: "Água" antes de "Leite")
    static final Comparator<Avaliado> ORDEM = Comparator
            .comparing((Avaliado a) -> !a.item().obrigatorio())
            .thenComparingDouble(Avaliado::cobertura)
            .thenComparing(Avaliado::faltante, Comparator.reverseOrder())
            .thenComparing(a -> a.item().nome(), Collator.getInstance(Locale.of("pt", "BR")));

    public static Resultado montar(List<Candidato> itens, List<Set<RestricaoAlimentar>> restricoesDosAtivos,
                                   Set<RestricaoAlimentar> minhasRestricoes, CalculoCobertura calculo) {
        var comRestricao = restricoesDosAtivos.stream().filter(r -> !r.isEmpty()).toList();
        var avaliados = new ArrayList<Avaliado>();
        int ocultos = 0;

        for (var item : itens) {
            long faltante = item.necessario() - item.contribuido();
            if (faltante <= 0) {
                continue;
            }
            long afetados = comRestricao.stream().filter(r -> conflita(r, item.alergenos())).count();
            // conflita com todos que têm restrição: ninguém com restrição poderia consumir
            if (!comRestricao.isEmpty() && afetados == comRestricao.size()) {
                ocultos++;
                continue;
            }
            var restricoes = EnumSet.noneOf(RestricaoAlimentar.class);
            comRestricao.forEach(r -> r.stream().filter(x -> x.conflitaCom(item.alergenos())).forEach(restricoes::add));
            var conflitos = new Conflitos(afetados, restricoes, conflita(minhasRestricoes, item.alergenos()));

            double cobertura = calculo.porItem(new CalculoCobertura.Item(item.necessario(), item.contribuido(), item.obrigatorio()));
            avaliados.add(new Avaliado(item, cobertura, faltante, conflitos));
        }

        var sugestoes = avaliados.stream().sorted(ORDEM).map(Sugestoes::paraSugestao).toList();
        return new Resultado(sugestoes, ocultos);
    }

    private static boolean conflita(Set<RestricaoAlimentar> restricoes, Set<Alergeno> alergenos) {
        return restricoes.stream().anyMatch(r -> r.conflitaCom(alergenos));
    }

    private static Sugestao paraSugestao(Avaliado a) {
        var item = a.item();
        return new Sugestao(item.itemId(), item.nome(), item.categoria(), item.unidade(), item.obrigatorio(),
                a.faltante(), CoberturaService.percentual(a.cobertura()), item.suaContribuicao(), a.conflitos());
    }
}
