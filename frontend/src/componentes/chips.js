import { criar } from '../dom.js';
import { rotulo } from '../logica/rotulos.js';

export function criarChips(destino, codigos, mapa, classe) {
  destino.replaceChildren(...codigos.map((codigo) => criar('span', `chip ${classe}`, rotulo(mapa, codigo))));
}
