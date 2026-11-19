export const TIPOS = {
  CAFE_DA_MANHA: 'Café da manhã',
  ALMOCO: 'Almoço',
  LANCHE: 'Lanche',
  CONFRATERNIZACAO: 'Confraternização',
  OUTRO: 'Outro',
};

export const STATUS = {
  RASCUNHO: 'Rascunho',
  ABERTO: 'Aberto',
  ENCERRADO: 'Encerrado',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
};

export const UNIDADES = {
  UNIDADE: 'unidade(s)',
  LITRO: 'litro(s)',
  KG: 'kg',
  PACOTE: 'pacote(s)',
  GARRAFA: 'garrafa(s)',
};

export const RESTRICOES = {
  VEGANO: 'Vegano',
  VEGETARIANO: 'Vegetariano',
  SEM_GLUTEN: 'Sem glúten',
  SEM_LACTOSE: 'Sem lactose',
  SEM_MEL: 'Sem mel',
  ALERGIA_AMENDOIM: 'Alergia a amendoim',
  ALERGIA_FRUTOS_DO_MAR: 'Alergia a frutos do mar',
  ALERGIA_OVO: 'Alergia a ovo',
  ALERGIA_PEIXE: 'Alergia a peixe',
};

// nomes dos campos da API como aparecem para o usuário nas mensagens de erro
export const CAMPOS = {
  nome: 'Nome',
  descricao: 'Descrição',
  tipo: 'Tipo',
  dataHora: 'Data e hora',
  prazoAlteracoes: 'Prazo para alterações',
  cpf: 'CPF',
  observacaoRestricao: 'Observação',
  meta: 'Meta',
  unidade: 'Unidade',
  categoriaId: 'Categoria',
  quantidadeNecessaria: 'Quantidade necessária',
  participanteId: 'Participante',
  itemId: 'Item',
  quantidade: 'Quantidade',
};

export const ALERGENOS = {
  GLUTEN: 'Glúten',
  LACTOSE: 'Lactose',
  OVO: 'Ovo',
  CARNE: 'Carne',
  PEIXE: 'Peixe',
  FRUTOS_DO_MAR: 'Frutos do mar',
  MEL: 'Mel',
  AMENDOIM: 'Amendoim',
};

export function rotulo(mapa, codigo) {
  return mapa[codigo] ?? codigo;
}

const ICONES_TIPO = {
  CAFE_DA_MANHA: 'bi-cup-hot',
  ALMOCO: 'bi-egg-fried',
  LANCHE: 'bi-basket',
  CONFRATERNIZACAO: 'bi-balloon',
  OUTRO: 'bi-calendar-event',
};

export function iconeDoTipo(tipo) {
  return ICONES_TIPO[tipo] ?? 'bi-calendar-event';
}

export function formatarDataHora(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

// para o bloquinho de calendário dos cards: "18" "dez" e a linha "sex, 08:00"
export function partesDaData(iso) {
  const data = new Date(iso);
  return {
    dia: String(data.getDate()).padStart(2, '0'),
    mes: data.toLocaleString('pt-BR', { month: 'short' }).replace('.', ''),
    semana: data.toLocaleString('pt-BR', { weekday: 'short' }).replace('.', ''),
    hora: data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
  };
}

export function formatarCpf(cpf) {
  return String(cpf ?? '').replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
}

// máscara enquanto a pessoa digita: "5299822" vira "529.982.2"
export function mascararCpfDigitado(texto) {
  const d = String(texto ?? '').replace(/\D/g, '').slice(0, 11);
  let resultado = d.slice(0, 3);
  if (d.length > 3) resultado += `.${d.slice(3, 6)}`;
  if (d.length > 6) resultado += `.${d.slice(6, 9)}`;
  if (d.length > 9) resultado += `-${d.slice(9)}`;
  return resultado;
}

// busca sem diferenciar maiúscula, minúscula e acento
export function normalizar(texto) {
  return String(texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

export function iniciais(nome) {
  const partes = String(nome ?? '').trim().split(/\s+/).filter(Boolean);
  if (!partes.length) return '?';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes.at(-1)[0] : '';
  return (primeira + ultima).toUpperCase();
}
