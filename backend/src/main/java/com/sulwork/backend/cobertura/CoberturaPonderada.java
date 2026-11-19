package com.sulwork.backend.cobertura;

import java.util.List;
import org.springframework.stereotype.Component;

// regra 10: obrigatório pesa 2, opcional pesa 1
@Component
public class CoberturaPonderada implements CalculoCobertura {

    static final int PESO_OBRIGATORIO = 2;
    static final int PESO_OPCIONAL = 1;

    @Override
    public double porItem(Item item) {
        return (double) Math.min(item.contribuido(), item.necessario()) / item.necessario();
    }

    @Override
    public double geral(List<Item> itens) {
        if (itens.isEmpty()) {
            return 0;
        }
        double somaPonderada = 0;
        int somaPesos = 0;
        for (var item : itens) {
            int peso = item.obrigatorio() ? PESO_OBRIGATORIO : PESO_OPCIONAL;
            somaPonderada += peso * porItem(item);
            somaPesos += peso;
        }
        return somaPonderada / somaPesos;
    }

    @Override
    public double porCategoria(Categoria categoria) {
        return Math.min(1.0, (double) categoria.contribuido() / categoria.meta());
    }
}
