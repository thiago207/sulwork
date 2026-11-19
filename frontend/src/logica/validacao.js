// espelha as regras do back para avisar antes de enviar; quem garante continua sendo a API
export function validarContribuicao({ participanteId, itemId, quantidade }, faltante) {
  const erros = {};
  if (!participanteId) erros.participanteId = 'Escolha o participante.';
  if (!itemId) erros.itemId = 'Escolha o item.';

  const numero = Number(quantidade);
  if (quantidade === '' || quantidade === null || quantidade === undefined) {
    erros.quantidade = 'Informe a quantidade.';
  } else if (!Number.isInteger(numero) || numero <= 0) {
    erros.quantidade = 'A quantidade deve ser um número inteiro maior que zero.';
  } else if (faltante !== undefined && numero > faltante) {
    erros.quantidade = `A quantidade passa do que falta para o item (faltam ${faltante}).`;
  }
  return erros;
}

// para meta e quantidade necessária: inteiro maior que zero; null quando está tudo certo
export function erroDeQuantidade(valor) {
  if (valor === '' || valor === null || valor === undefined) return 'Informe um valor.';
  const numero = Number(valor);
  if (!Number.isInteger(numero)) return 'Use um número inteiro, sem casas decimais.';
  if (numero <= 0) return 'Deve ser maior que zero.';
  return null;
}

// "" vira null (campo vazio), mas "0" continua 0: assim o back responde "maior que zero" e não "obrigatório"
export function paraNumero(valor) {
  return valor === '' || valor === null || valor === undefined ? null : Number(valor);
}
