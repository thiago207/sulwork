import modeloAlerta from './componentes/alerta.html?raw';
import { campo, criar, instanciar, preencher } from './dom.js';
import { CAMPOS, rotulo } from './logica/rotulos.js';

const ICONES = {
  danger: 'bi-x-circle-fill',
  success: 'bi-check-circle-fill',
  warning: 'bi-exclamation-triangle-fill',
};

export function limparAlertas() {
  document.getElementById('alertas')?.replaceChildren();
}

function mostrarAlerta(tipo, { titulo = '', mensagem = '', itens = [] }) {
  const area = document.getElementById('alertas');
  if (!area) return null;
  const alerta = instanciar(modeloAlerta);
  alerta.classList.add(`alert-${tipo}`);
  alerta.querySelector('[data-icone]').classList.add(ICONES[tipo]);
  preencher(alerta, { titulo, mensagem });
  campo(alerta, 'titulo').hidden = !titulo;
  campo(alerta, 'mensagem').hidden = !mensagem;
  campo(alerta, 'itens').replaceChildren(...itens.map((texto) => criar('li', '', texto)));
  campo(alerta, 'itens').hidden = !itens.length;
  area.append(alerta);
  return alerta;
}

export function mostrarErro(erro) {
  mostrarAlerta('danger', {
    mensagem: erro.message ?? 'Erro inesperado.',
    itens: (erro.campos ?? []).map((c) => `${rotulo(CAMPOS, c.campo)}: ${c.mensagem}`),
  });
}

export function mostrarSucesso(mensagem) {
  const alerta = mostrarAlerta('success', { mensagem });
  // sucesso some sozinho; erro e aviso ficam até a pessoa fechar
  setTimeout(() => alerta?.remove(), 4000);
}

export function mostrarAvisos(avisos) {
  if (!avisos?.length) return;
  mostrarAlerta('warning', { titulo: 'Atenção aos alérgenos', itens: avisos });
}

// o roteador limpa os alertas ao trocar de tela; esta mensagem aparece depois que a próxima tela carregar
let sucessoPendente = null;

export function sucessoNaProximaTela(mensagem) {
  sucessoPendente = mensagem;
}

export function mostrarSucessoPendente() {
  if (sucessoPendente) mostrarSucesso(sucessoPendente);
  sucessoPendente = null;
}

// erros: { nomeDoCampo: 'mensagem' }; a mensagem aparece no <div data-erro="nomeDoCampo">
export function marcarErros(form, erros) {
  form.querySelectorAll('[name]').forEach((entrada) => entrada.classList.toggle('is-invalid', Boolean(erros[entrada.name])));
  form.querySelectorAll('[data-erro]').forEach((div) => { div.textContent = erros[div.dataset.erro] ?? ''; });
}

// campos que existem no formulário ficam vermelhos com a mensagem embaixo; o resto vai para o alerta
export function mostrarErroNoForm(form, erro) {
  const campos = erro.campos ?? [];
  const noForm = campos.filter((c) => form.elements[c.campo]);
  marcarErros(form, Object.fromEntries(noForm.map((c) => [c.campo, c.mensagem])));
  mostrarErro({ message: erro.message, campos: campos.filter((c) => !form.elements[c.campo]) });
}

export async function tentar(acao) {
  try {
    return await acao();
  } catch (erro) {
    mostrarErro(erro);
    return undefined;
  }
}
