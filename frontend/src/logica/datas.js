const calendarioRecife = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'America/Recife',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  fractionalSecondDigits: 3,
  hourCycle: 'h23',
});

export function dataLocalRecife(instante) {
  const partes = Object.fromEntries(calendarioRecife.formatToParts(instante).map(({ type, value }) => [type, value]));
  const { year, month, day, hour, minute, second, fractionalSecond } = partes;
  return `${year}-${month}-${day}T${hour}:${minute}:${second}.${fractionalSecond}`;
}

// A API envia horário civil de Recife. UTC aqui apenas preserva os campos do calendário.
export function dataParaCalendario(isoLocal) {
  return new Date(isoLocal + 'Z');
}

export function compararComDataLocal(instante, isoLocal) {
  return dataParaCalendario(dataLocalRecife(instante)) - dataParaCalendario(isoLocal);
}
