import modelo from './confirmacao.html?raw';
import { Modal } from 'bootstrap';
import { instanciar, preencher } from '../dom.js';

// abre o modal e devolve uma Promise: true se a pessoa confirmou, false se voltou
export function confirmar({ titulo, texto = '', botao = 'Confirmar', estilo = 'danger' }) {
  return new Promise((resolver) => {
    const elemento = instanciar(modelo);
    preencher(elemento, { titulo, texto });
    elemento.querySelector('.confirmacao').dataset.estilo = estilo;
    const botaoConfirmar = elemento.querySelector('[data-confirmar]');
    botaoConfirmar.textContent = botao;
    botaoConfirmar.classList.add(`btn-${estilo}`);

    const modal = new Modal(elemento);
    let confirmado = false;
    botaoConfirmar.addEventListener('click', () => {
      confirmado = true;
      modal.hide();
    });
    elemento.addEventListener('hidden.bs.modal', () => {
      modal.dispose();
      elemento.remove();
      resolver(confirmado);
    });
    document.body.append(elemento);
    modal.show();
  });
}
