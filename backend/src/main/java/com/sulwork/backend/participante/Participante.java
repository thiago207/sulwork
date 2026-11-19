package com.sulwork.backend.participante;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cpf;
    private String observacaoRestricao;
    private boolean ativo = true;

    @ElementCollection
    @CollectionTable(name = "participante_restricao", joinColumns = @JoinColumn(name = "participante_id"))
    @Column(name = "restricao")
    @Enumerated(EnumType.STRING)
    private Set<RestricaoAlimentar> restricoes = new HashSet<>();

    public Participante(String nome, String cpf, Set<RestricaoAlimentar> restricoes, String observacaoRestricao) {
        atualizar(nome, cpf, restricoes, observacaoRestricao);
    }

    public void atualizar(String nome, String cpf, Set<RestricaoAlimentar> restricoes, String observacaoRestricao) {
        this.nome = nome;
        this.cpf = cpf;
        this.observacaoRestricao = observacaoRestricao;
        // não troca a coleção: o Hibernate acompanha a instância original
        this.restricoes.clear();
        if (restricoes != null) {
            this.restricoes.addAll(restricoes);
        }
    }

    public void desativar() {
        this.ativo = false;
    }

    // quem foi excluído e se cadastra de novo volta com os dados novos
    public void reativar(String nome, Set<RestricaoAlimentar> restricoes, String observacaoRestricao) {
        atualizar(nome, cpf, restricoes, observacaoRestricao);
        this.ativo = true;
    }
}
