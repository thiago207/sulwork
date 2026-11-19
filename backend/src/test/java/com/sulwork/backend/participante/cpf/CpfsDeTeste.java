package com.sulwork.backend.participante.cpf;

import java.util.concurrent.ThreadLocalRandom;

public final class CpfsDeTeste {

    private CpfsDeTeste() {
    }

    // o banco é compartilhado entre as classes de teste, então cada CPF precisa ser novo
    public static String aleatorio() {
        String base = String.format("%09d", ThreadLocalRandom.current().nextInt(1, 999_999_999));
        for (int dv = 0; dv < 100; dv++) {
            String cpf = base + String.format("%02d", dv);
            if (CpfValidator.valido(cpf)) {
                return cpf;
            }
        }
        return aleatorio();
    }
}
