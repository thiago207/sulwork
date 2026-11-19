package com.sulwork.backend.contribuicao;

import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.item.Item;
import com.sulwork.backend.participacao.Participacao;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contribuicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Participacao participacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Item item;

    private int quantidade;
    private boolean ativo = true;

    public Contribuicao(Participacao participacao, Item item, int quantidade) {
        this.participacao = participacao;
        this.item = item;
        this.quantidade = quantidade;
    }

    public void alterarQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public void remover() {
        this.ativo = false;
    }

    // regra 11: avisa, sem bloquear, quando o item conflita com a restrição de quem está levando
    public List<String> avisosDeAlergeno() {
        return participacao.getParticipante().getRestricoes().stream()
                .filter(restricao -> restricao.conflitaCom(item.getAlergenos()))
                .sorted()
                .map(restricao -> {
                    String alergenos = item.getAlergenos().stream()
                            .filter(restricao.getAlergenos()::contains)
                            .sorted()
                            .map(Alergeno::getRotulo)
                            .collect(Collectors.joining(", "));
                    return "O item " + item.getNome() + " contém " + alergenos
                            + ", que conflita com sua restrição: " + restricao.getRotulo() + ".";
                })
                .toList();
    }
}
