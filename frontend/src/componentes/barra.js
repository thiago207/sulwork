import modelo from './barra.html?raw';
import { instanciar } from '../dom.js';
import { classeBarra, formatarPercentual, larguraBarra } from '../logica/cobertura.js';

export function criarBarra(percentual) {
  const barra = instanciar(modelo);
  const largura = larguraBarra(percentual);
  barra.querySelector('.progress').setAttribute('aria-valuenow', largura);
  const preenchimento = barra.querySelector('.progress-bar');
  preenchimento.classList.add(classeBarra(percentual));
  preenchimento.style.width = `${largura}%`;
  barra.querySelector('.barra-valor').textContent = formatarPercentual(percentual);
  return barra;
}

// troca o marcador <div data-barra> de um modelo pela barra de verdade
export function colocarBarra(elemento, percentual) {
  elemento.querySelector('[data-barra]').replaceWith(criarBarra(percentual));
}
