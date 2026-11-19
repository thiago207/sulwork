import modelo from './opcaoChip.html?raw';
import { instanciar } from '../dom.js';

// todos os checkboxes têm o mesmo name, então o FormData.getAll(name) devolve os marcados
export function criarOpcoesChip(destino, nome, mapa) {
  destino.replaceChildren(...Object.entries(mapa).map(([valor, texto]) => {
    const opcao = instanciar(modelo);
    const entrada = opcao.querySelector('input');
    const etiqueta = opcao.querySelector('label');
    entrada.id = `${nome}-${valor}`;
    entrada.name = nome;
    entrada.value = valor;
    etiqueta.htmlFor = entrada.id;
    etiqueta.textContent = texto;
    return opcao;
  }));
}
