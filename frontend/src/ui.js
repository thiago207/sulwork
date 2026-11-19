// todo texto vindo da API passa por aqui antes de ir para o innerHTML (evita XSS)
export function escapar(valor) {
  return String(valor ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function areaAlertas() {
  return document.getElementById('alertas');
}

export function limparAlertas() {
  const area = areaAlertas();
  if (area) area.innerHTML = '';
}

export function mostrarAlerta(tipo, conteudoHtml) {
  const area = areaAlertas();
  if (!area) return;
  area.insertAdjacentHTML('beforeend', `
    <div class="alert alert-${tipo} alert-dismissible" role="alert">
      ${conteudoHtml}
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
    </div>`);
}

export function mostrarErro(erro) {
  const campos = erro.campos?.length
    ? `<ul class="mb-0 mt-2">${erro.campos.map((c) => `<li><strong>${escapar(c.campo)}</strong>: ${escapar(c.mensagem)}</li>`).join('')}</ul>`
    : '';
  mostrarAlerta('danger', `${escapar(erro.message ?? 'Erro inesperado.')}${campos}`);
}

export function mostrarSucesso(mensagem) {
  mostrarAlerta('success', escapar(mensagem));
}

export function mostrarAvisos(avisos) {
  if (!avisos?.length) return;
  mostrarAlerta('warning', `
    <h6 class="alert-heading fw-bold">⚠ Atenção aos alérgenos</h6>
    <ul class="mb-0">${avisos.map((a) => `<li>${escapar(a)}</li>`).join('')}</ul>`);
}

// executa uma ação da API e mostra o erro em alerta, sem deixar a exceção escapar
export async function tentar(acao) {
  try {
    return await acao();
  } catch (erro) {
    mostrarErro(erro);
    return undefined;
  }
}

export function opcoes(lista, valorAtual = '') {
  return lista
    .map(([valor, texto]) => `<option value="${escapar(valor)}" ${String(valor) === String(valorAtual) ? 'selected' : ''}>${escapar(texto)}</option>`)
    .join('');
}
