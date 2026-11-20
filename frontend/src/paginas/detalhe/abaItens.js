import modelo from './abaItens.html?raw';
import { api } from '../../api.js';
import { marcarErros } from '../../ui.js';
import { campo, clonar, preencher, preencherSelect, renderizar } from '../../dom.js';
import { criarChips } from '../../componentes/chips.js';
import { criarOpcoesChip } from '../../componentes/opcoesChip.js';
import { confirmar } from '../../componentes/confirmacao.js';
import { ALERGENOS, UNIDADES, rotulo } from '../../logica/rotulos.js';
import { erroDeQuantidade, paraNumero } from '../../logica/validacao.js';

function criarItem(aba, item, editavel) {
  const linha = clonar(aba, 'item');
  preencher(linha, {
    nome: item.nome,
    quantidade: `${item.quantidadeNecessaria} ${rotulo(UNIDADES, item.unidade)}`,
  });
  campo(linha, 'obrigatorio').hidden = !item.obrigatorio;
  criarChips(campo(linha, 'alergenos'), item.alergenos, ALERGENOS, 'chip-alergeno');
  if (!editavel) campo(linha, 'acoes').remove();
  else campo(linha, 'acoes').dataset.id = item.id;
  return linha;
}

function criarCategoria(aba, categoria, itens, editavel) {
  const cartao = clonar(aba, 'categoria');
  const unidade = rotulo(UNIDADES, categoria.unidade);
  // a cobertura da categoria é contribuído / meta; se os itens pedem menos que a meta, ela nunca chega a 100%
  const pedido = itens.reduce((soma, i) => soma + i.quantidadeNecessaria, 0);
  preencher(cartao, {
    nome: categoria.nome,
    meta: `Meta: ${categoria.meta} ${unidade}`,
    'alerta-texto': `Os itens pedem só ${pedido} ${unidade} no total; a categoria não chega na meta.`,
  });
  campo(cartao, 'alerta').hidden = !itens.length || pedido >= categoria.meta;
  campo(cartao, 'itens').replaceChildren(...itens.map((i) => criarItem(aba, i, editavel)));
  campo(cartao, 'vazia').hidden = itens.length > 0;
  if (!editavel) campo(cartao, 'acoes').remove();
  else campo(cartao, 'acoes').dataset.id = categoria.id;
  return cartao;
}

// valida no navegador antes de mandar; devolve true se pode enviar
function validarNumeros(form, nomes) {
  const erros = {};
  for (const nome of nomes) {
    const erro = erroDeQuantidade(form.elements[nome].value);
    if (erro) erros[nome] = erro;
  }
  marcarErros(form, erros);
  return !Object.keys(erros).length;
}

export function desenharItens(aba, { categorias, itens, editavel }, { id, executar }) {
  renderizar(aba, modelo);
  aba.querySelector('#categorias').replaceChildren(
    ...categorias.map((c) => criarCategoria(aba, c, itens.filter((i) => i.categoriaId === c.id), editavel)),
  );
  aba.querySelector('#sem-categorias').hidden = categorias.length > 0;
  aba.querySelector('#formularios-itens').hidden = !editavel;
  if (!editavel) return;

  const formCategoria = aba.querySelector('#form-categoria');
  const formItem = aba.querySelector('#form-item');
  let categoriaEmEdicao;
  let itemEmEdicao;
  preencherSelect(formCategoria.elements.unidade, Object.entries(UNIDADES));
  preencherSelect(formItem.elements.categoriaId, categorias.map((c) => [c.id, c.nome]));
  criarOpcoesChip(aba.querySelector('#opcoes-alergenos'), 'alergenos', ALERGENOS);
  // item precisa de categoria, então o formulário só aparece quando existe alguma
  aba.querySelector('#coluna-item').hidden = !categorias.length;

  const mostrarUnidade = () => {
    const escolhida = categorias.find((c) => String(c.id) === formItem.elements.categoriaId.value);
    aba.querySelector('#item-unidade').textContent = escolhida ? rotulo(UNIDADES, escolhida.unidade) : '';
  };
  formItem.elements.categoriaId.addEventListener('change', mostrarUnidade);
  mostrarUnidade();

  const modoEdicaoCategoria = (categoria) => {
    categoriaEmEdicao = categoria;
    formCategoria.reset();
    marcarErros(formCategoria, {});
    formCategoria.querySelector('h3').textContent = categoria ? 'Editar categoria' : 'Nova categoria';
    formCategoria.querySelector('[type="submit"]').textContent = categoria ? 'Salvar categoria' : 'Adicionar categoria';
    formCategoria.querySelector('[data-cancelar]').hidden = !categoria;
    if (categoria) {
      formCategoria.elements.nome.value = categoria.nome;
      formCategoria.elements.meta.value = categoria.meta;
      formCategoria.elements.unidade.value = categoria.unidade;
      formCategoria.elements.nome.focus();
    }
  };

  const modoEdicaoItem = (item) => {
    itemEmEdicao = item;
    formItem.reset();
    marcarErros(formItem, {});
    formItem.querySelector('h3').textContent = item ? 'Editar item' : 'Novo item';
    formItem.querySelector('[type="submit"]').textContent = item ? 'Salvar item' : 'Adicionar item';
    formItem.querySelector('[data-cancelar]').hidden = !item;
    if (item) {
      formItem.elements.nome.value = item.nome;
      formItem.elements.categoriaId.value = item.categoriaId;
      formItem.elements.quantidadeNecessaria.value = item.quantidadeNecessaria;
      formItem.elements.obrigatorio.checked = item.obrigatorio;
      formItem.querySelectorAll('input[name="alergenos"]').forEach((campo) => {
        campo.checked = item.alergenos.includes(campo.value);
      });
      formItem.elements.nome.focus();
    }
    mostrarUnidade();
  };
  formCategoria.querySelector('[data-cancelar]').addEventListener('click', () => modoEdicaoCategoria());
  formItem.querySelector('[data-cancelar]').addEventListener('click', () => modoEdicaoItem());

  formCategoria.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validarNumeros(formCategoria, ['meta'])) return;
    const dados = new FormData(formCategoria);
    const requisicao = {
      nome: dados.get('nome'),
      meta: paraNumero(dados.get('meta')),
      unidade: dados.get('unidade'),
    };
    executar(() => categoriaEmEdicao
      ? api.editarCategoria(id, categoriaEmEdicao.id, requisicao)
      : api.criarCategoria(id, requisicao), formCategoria);
  });

  formItem.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validarNumeros(formItem, ['quantidadeNecessaria'])) return;
    const dados = new FormData(formItem);
    const requisicao = {
      nome: dados.get('nome'),
      categoriaId: Number(dados.get('categoriaId')),
      quantidadeNecessaria: paraNumero(dados.get('quantidadeNecessaria')),
      obrigatorio: dados.has('obrigatorio'),
      alergenos: dados.getAll('alergenos'),
    };
    executar(() => itemEmEdicao
      ? api.editarItem(id, itemEmEdicao.id, requisicao)
      : api.criarItem(id, requisicao), formItem);
  });

  aba.querySelector('#categorias').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    const alvo = botao.closest('[data-id]').dataset.id;
    const { acao } = botao.dataset;
    if (acao.endsWith('-categoria')) {
      const categoria = categorias.find((c) => String(c.id) === alvo);
      if (acao === 'editar-categoria') modoEdicaoCategoria(categoria);
      else if (await confirmar({ titulo: `Remover a categoria "${categoria.nome}"?`, texto: 'Ela precisa estar sem itens.', botao: 'Remover' })) {
        executar(() => api.excluirCategoria(id, alvo));
      }
    } else {
      const item = itens.find((i) => String(i.id) === alvo);
      if (acao === 'editar-item') modoEdicaoItem(item);
      else if (await confirmar({ titulo: `Remover o item "${item.nome}"?`, botao: 'Remover' })) {
        executar(() => api.excluirItem(id, alvo));
      }
    }
  });
}
