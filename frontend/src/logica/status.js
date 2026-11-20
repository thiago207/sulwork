import { compararComDataLocal } from './datas.js';

// o back manda proximosStatus; aqui escondemos o que ele recusaria pela data
export function transicoesVisiveis(evento, agora) {
  return (evento.proximosStatus ?? []).filter((destino) => {
    if (evento.status === 'ENCERRADO' && destino === 'ABERTO') return compararComDataLocal(agora, evento.prazoAlteracoes) < 0;
    if (destino === 'REALIZADO') return compararComDataLocal(agora, evento.dataHora) >= 0;
    return true;
  });
}

export function botaoDaTransicao(origem, destino) {
  if (destino === 'ABERTO') return { texto: origem === 'ENCERRADO' ? 'Reabrir' : 'Abrir', estilo: 'success' };
  if (destino === 'ENCERRADO') return { texto: 'Encerrar', estilo: 'warning' };
  if (destino === 'REALIZADO') return { texto: 'Marcar como realizado', estilo: 'primary' };
  if (destino === 'CANCELADO') return { texto: 'Cancelar evento', estilo: 'outline-danger' };
  return { texto: destino, estilo: 'secondary' };
}

// texto do modal de confirmação de cada mudança de status
export function confirmacaoDaTransicao(origem, destino) {
  const final = 'Essa ação é final: depois disso o evento não muda mais.';
  if (destino === 'ABERTO' && origem === 'ENCERRADO') {
    return { titulo: 'Reabrir o evento?', texto: 'Participantes e contribuições voltam a poder mudar até o prazo.', botao: 'Reabrir', estilo: 'success' };
  }
  if (destino === 'ABERTO') {
    return { titulo: 'Abrir o evento?', texto: 'Os participantes poderão entrar e escolher o que levar até o prazo de alterações.', botao: 'Abrir evento', estilo: 'success' };
  }
  if (destino === 'ENCERRADO') {
    return { titulo: 'Encerrar o evento?', texto: 'Ninguém mais entra, sai ou muda contribuições. Dá para reabrir enquanto o prazo não vencer.', botao: 'Encerrar', estilo: 'warning' };
  }
  if (destino === 'REALIZADO') {
    return { titulo: 'Marcar como realizado?', texto: final, botao: 'Marcar como realizado', estilo: 'primary' };
  }
  return { titulo: 'Cancelar o evento?', texto: final, botao: 'Cancelar evento', estilo: 'danger' };
}

export function podeEditar(evento) {
  return evento.status === 'RASCUNHO' || evento.status === 'ABERTO';
}

export function aceitaContribuicoes(evento, agora) {
  return evento.status === 'ABERTO' && compararComDataLocal(agora, evento.prazoAlteracoes) < 0;
}
