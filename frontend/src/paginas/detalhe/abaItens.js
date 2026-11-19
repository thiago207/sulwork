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
  const remover = linha.querySelector('[data-acao]');
  remover.dataset.id = item.id;
  remover.dataset.nome = item.nome;
  remover.hidden = !editavel;
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
  const remover = cartao.querySelector('[data-acao]');
  remover.dataset.id = categoria.id;
  remover.dataset.nome = categoria.nome;
  remover.hidden = !editavel;
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

  formCategoria.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validarNumeros(formCategoria, ['meta'])) return;
    const dados = new FormData(formCategoria);
    executar(() => api.criarCategoria(id, {
      nome: dados.get('nome'),
      meta: paraNumero(dados.get('meta')),
      unidade: dados.get('unidade'),
    }), formCategoria);
  });

  formItem.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validarNumeros(formItem, ['quantidadeNecessaria'])) return;
    const dados = new FormData(formItem);
    executar(() => api.criarItem(id, {
      nome: dados.get('nome'),
      categoriaId: Number(dados.get('categoriaId')),
      quantidadeNecessaria: paraNumero(dados.get('quantidadeNecessaria')),
      obrigatorio: dados.has('obrigatorio'),
      alergenos: dados.getAll('alergenos'),
    }), formItem);
  });

  aba.querySelector('#categorias').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    const { acao, id: alvo, nome } = botao.dataset;
    if (acao === 'excluir-categoria'
        && await confirmar({ titulo: `Remover a categoria "${nome}"?`, texto: 'Ela precisa estar sem itens.', botao: 'Remover' })) {
      executar(() => api.excluirCategoria(id, alvo));
    }
    if (acao === 'excluir-item'
        && await confirmar({ titulo: `Remover o item "${nome}"?`, botao: 'Remover' })) {
      executar(() => api.excluirItem(id, alvo));
    }
  });
}
