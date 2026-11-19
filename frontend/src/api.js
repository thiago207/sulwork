const BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080') + '/api';

// erro no formato do TratadorGlobalExcecoes do back: status, mensagem e campos
export class ErroApi extends Error {
  constructor(status, mensagem, campos = []) {
    super(mensagem);
    this.status = status;
    this.campos = campos;
  }
}

async function requisitar(metodo, caminho, corpo) {
  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      method: metodo,
      headers: corpo === undefined ? {} : { 'Content-Type': 'application/json' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    // fetch só rejeita em falha de rede: API fora do ar, CORS bloqueado...
    throw new ErroApi(0, 'Não foi possível conectar à API.');
  }

  if (resposta.status === 204) {
    return null;
  }
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, dados?.mensagem ?? `Erro ${resposta.status} na API.`, dados?.campos ?? []);
  }
  return dados;
}

const get = (caminho) => requisitar('GET', caminho);
const post = (caminho, corpo = {}) => requisitar('POST', caminho, corpo);
const put = (caminho, corpo) => requisitar('PUT', caminho, corpo);
const patch = (caminho, corpo) => requisitar('PATCH', caminho, corpo);
const remover = (caminho) => requisitar('DELETE', caminho);

export const api = {
  listarEventos: (status) => get('/eventos' + (status ? `?status=${status}` : '')),
  buscarEvento: (id) => get(`/eventos/${id}`),
  criarEvento: (dados) => post('/eventos', dados),
  editarEvento: (id, dados) => put(`/eventos/${id}`, dados),
  excluirEvento: (id) => remover(`/eventos/${id}`),
  mudarStatus: (id, status) => patch(`/eventos/${id}/status`, { status }),

  listarCategorias: (eventoId) => get(`/eventos/${eventoId}/categorias`),
  criarCategoria: (eventoId, dados) => post(`/eventos/${eventoId}/categorias`, dados),
  excluirCategoria: (eventoId, id) => remover(`/eventos/${eventoId}/categorias/${id}`),

  listarItens: (eventoId) => get(`/eventos/${eventoId}/itens`),
  criarItem: (eventoId, dados) => post(`/eventos/${eventoId}/itens`, dados),
  excluirItem: (eventoId, id) => remover(`/eventos/${eventoId}/itens/${id}`),

  listarParticipantes: () => get('/participantes'),
  criarParticipante: (dados) => post('/participantes', dados),
  editarParticipante: (id, dados) => put(`/participantes/${id}`, dados),
  excluirParticipante: (id) => remover(`/participantes/${id}`),

  listarParticipantesDoEvento: (eventoId) => get(`/eventos/${eventoId}/participantes`),
  entrarNoEvento: (eventoId, participanteId) => post(`/eventos/${eventoId}/participantes/${participanteId}`),
  sairDoEvento: (eventoId, participanteId) => remover(`/eventos/${eventoId}/participantes/${participanteId}`),

  listarContribuicoes: (eventoId) => get(`/eventos/${eventoId}/contribuicoes`),
  criarContribuicao: (eventoId, dados) => post(`/eventos/${eventoId}/contribuicoes`, dados),
  removerContribuicao: (eventoId, id) => remover(`/eventos/${eventoId}/contribuicoes/${id}`),

  cobertura: (eventoId) => get(`/eventos/${eventoId}/cobertura`),
  restricoes: (eventoId) => get(`/eventos/${eventoId}/restricoes`),
  sugestoes: (eventoId, participanteId) => get(`/eventos/${eventoId}/participantes/${participanteId}/sugestoes`),
};
