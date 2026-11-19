// transforma o texto de um arquivo .html em elementos, sem exibir nada ainda
export function montar(html) {
  const template = document.createElement('template');
  template.innerHTML = html;
  return template.content;
}

export function instanciar(html) {
  return montar(html).firstElementChild;
}

export function renderizar(container, html) {
  container.replaceChildren(montar(html));
}

// copia um <template data-modelo="..."> que está dentro da página
export function clonar(raiz, nome) {
  return raiz.querySelector(`template[data-modelo="${nome}"]`).content.firstElementChild.cloneNode(true);
}

export function campo(elemento, nome) {
  return elemento.querySelector(`[data-campo="${nome}"]`);
}

// textContent mostra o valor sempre como texto, nunca como HTML (protege contra XSS)
export function preencher(elemento, valores) {
  for (const [nome, valor] of Object.entries(valores)) {
    campo(elemento, nome).textContent = valor ?? '';
  }
  return elemento;
}

export function criar(tag, classe, texto) {
  const elemento = document.createElement(tag);
  if (classe) elemento.className = classe;
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}

export function preencherSelect(select, opcoes) {
  for (const [valor, texto] of opcoes) {
    const opcao = criar('option', '', texto);
    opcao.value = valor;
    select.append(opcao);
  }
}
