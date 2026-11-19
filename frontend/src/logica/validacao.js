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
