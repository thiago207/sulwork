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
  ALERGIA_AMENDOIM: 'Alergia a amendoim',
  ALERGIA_FRUTOS_DO_MAR: 'Alergia a frutos do mar',
  ALERGIA_OVO: 'Alergia a ovo',
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

export function formatarDataHora(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}
