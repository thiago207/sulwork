package com.sulwork.backend.item;

import com.sulwork.backend.categoria.Categoria;
import com.sulwork.backend.categoria.UnidadeMedida;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Categoria categoria;

    private String nome;
    private int quantidadeNecessaria;
    private boolean obrigatorio;

    @ElementCollection
    @CollectionTable(name = "item_alergeno", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "alergeno")
    @Enumerated(EnumType.STRING)
    private Set<Alergeno> alergenos = new HashSet<>();

    public Item(Categoria categoria, String nome, int quantidadeNecessaria, boolean obrigatorio, Set<Alergeno> alergenos) {
        atualizar(categoria, nome, quantidadeNecessaria, obrigatorio, alergenos, 0);
    }

    // regra 8: não pode ficar abaixo do que já foi contribuído
    public void atualizar(Categoria categoria, String nome, int quantidadeNecessaria, boolean obrigatorio,
                          Set<Alergeno> alergenos, int totalContribuido) {
        if (quantidadeNecessaria < totalContribuido) {
            throw new RegraNegocioException("A quantidade necessária não pode ser menor que o total já contribuído ("
                    + totalContribuido + ").");
        }
        this.categoria = categoria;
        this.nome = nome;
        this.quantidadeNecessaria = quantidadeNecessaria;
        this.obrigatorio = obrigatorio;
        this.alergenos.clear();
        if (alergenos != null) {
            this.alergenos.addAll(alergenos);
        }
    }

    public UnidadeMedida getUnidade() {
        return categoria.getUnidade();
    }
}
