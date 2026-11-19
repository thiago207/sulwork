const CORES = {
  RASCUNHO: 'secondary',
  ABERTO: 'success',
  ENCERRADO: 'warning',
  REALIZADO: 'primary',
  CANCELADO: 'danger',
};

export function corDoStatus(status) {
  return CORES[status] ?? 'secondary';
}

// o back manda proximosStatus; aqui só escondemos "reabrir" depois do prazo, que o back também recusaria
export function transicoesVisiveis(evento, agora) {
  return (evento.proximosStatus ?? []).filter(
    (destino) => !(evento.status === 'ENCERRADO' && destino === 'ABERTO' && agora >= new Date(evento.prazoAlteracoes)),
  );
}

export function botaoDaTransicao(origem, destino) {
  if (destino === 'ABERTO') return { texto: origem === 'ENCERRADO' ? 'Reabrir' : 'Abrir', estilo: 'success' };
  if (destino === 'ENCERRADO') return { texto: 'Encerrar', estilo: 'warning' };
  if (destino === 'REALIZADO') return { texto: 'Marcar como realizado', estilo: 'primary' };
  if (destino === 'CANCELADO') return { texto: 'Cancelar evento', estilo: 'outline-danger' };
  return { texto: destino, estilo: 'secondary' };
}

export function podeEditar(evento) {
  return evento.status === 'RASCUNHO' || evento.status === 'ABERTO';
}

export function aceitaContribuicoes(evento, agora) {
  return evento.status === 'ABERTO' && agora < new Date(evento.prazoAlteracoes);
}
