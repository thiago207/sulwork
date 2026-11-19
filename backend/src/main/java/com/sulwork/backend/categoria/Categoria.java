package com.sulwork.backend.categoria;

import com.sulwork.backend.evento.Evento;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Evento evento;

    private String nome;
    private int meta;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidade;

    public Categoria(Evento evento, String nome, int meta, UnidadeMedida unidade) {
        this.evento = evento;
        atualizar(nome, meta, unidade);
    }

    public void atualizar(String nome, int meta, UnidadeMedida unidade) {
        this.nome = nome;
        this.meta = meta;
        this.unidade = unidade;
    }
}
