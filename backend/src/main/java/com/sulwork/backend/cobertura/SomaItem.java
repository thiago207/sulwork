package com.sulwork.backend.cobertura;

// projection da consulta nativa: cada getter corresponde a um alias do SQL
public interface SomaItem {

    Long getItemId();

    String getNome();

    Boolean getObrigatorio();

    Integer getNecessario();

    Long getCategoriaId();

    String getUnidade();

    Long getContribuido();
}
