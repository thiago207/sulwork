// a API manda percentuais de 0 a 100 com uma casa decimal (ex.: 62.5)
export function formatarPercentual(valor) {
  const numero = Number(valor ?? 0);
  return numero.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
}

export function classeBarra(percentual) {
  if (percentual >= 100) return 'bg-success';
  if (percentual >= 50) return 'bg-warning';
  return 'bg-danger';
}

// largura da barra sempre entre 0 e 100
export function larguraBarra(percentual) {
  return Math.min(100, Math.max(0, Number(percentual ?? 0)));
}
