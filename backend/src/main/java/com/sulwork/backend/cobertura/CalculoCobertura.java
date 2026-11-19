package com.sulwork.backend.cobertura;

import java.util.List;

// Strategy: o service depende só desta interface; a fórmula pode ser trocada sem mexer nele
public interface CalculoCobertura {

    // frações de 0 a 1
    double porItem(Item item);

    double geral(List<Item> itens);

    double porCategoria(Categoria categoria);

    record Item(int necessario, long contribuido, boolean obrigatorio) {
    }

    record Categoria(int meta, long contribuido) {
    }
}
