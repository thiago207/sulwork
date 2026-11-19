import { api } from '../api.js';
import { escapar, limparAlertas, mostrarAvisos, mostrarErro, mostrarSucesso, opcoes } from '../ui.js';
import { ALERGENOS, RESTRICOES, STATUS, TIPOS, UNIDADES, formatarDataHora, rotulo } from '../logica/rotulos.js';
import { aceitaContribuicoes, botaoDaTransicao, corDoStatus, podeEditar, transicoesVisiveis } from '../logica/status.js';
import { classeBarra, formatarPercentual, larguraBarra } from '../logica/cobertura.js';
import { validarContribuicao } from '../logica/validacao.js';

const ABAS = [
  ['itens', 'Itens'],
  ['participantes', 'Participantes'],
  ['contribuicoes', 'Contribuições'],
  ['cobertura', 'Cobertura'],
  ['restricoes', 'Restrições e alérgenos'],
];
let abaAtiva = 'itens';

export function htmlBarra(percentual) {
  const largura = larguraBarra(percentual);
  return `
    <div class="progress" role="progressbar" aria-valuenow="${largura}" aria-valuemin="0" aria-valuemax="100">
      <div class="progress-bar ${classeBarra(percentual)}" style="width: ${largura}%">${formatarPercentual(percentual)}</div>
    </div>`;
}

const badgesAlergenos = (lista) =>
  lista.map((a) => `<span class="badge text-bg-warning me-1">${escapar(rotulo(ALERGENOS, a))}</span>`).join('')
  || '<span class="text-muted">nenhum</span>';

const badgesRestricoes = (lista) =>
  lista.map((r) => `<span class="badge text-bg-info me-1">${escapar(rotulo(RESTRICOES, r))}</span>`).join('');

function htmlCabecalho(evento, agora) {
  const botoesStatus = transicoesVisiveis(evento, agora).map((destino) => {
    const { texto, estilo } = botaoDaTransicao(evento.status, destino);
    return `<button class="btn btn-sm btn-${estilo}" data-acao="status" data-status="${destino}">${texto}</button>`;
  }).join('');
  return `
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
      <div>
        <h1 class="h3 mb-1">${escapar(evento.nome)}
          <span class="badge text-bg-${corDoStatus(evento.status)} align-middle fs-6">${escapar(rotulo(STATUS, evento.status))}</span>
        </h1>
        <div class="text-muted">
          ${escapar(rotulo(TIPOS, evento.tipo))} · ${escapar(formatarDataHora(evento.dataHora))}
          · alterações até ${escapar(formatarDataHora(evento.prazoAlteracoes))}
        </div>
        ${evento.descricao ? `<p class="mt-2 mb-0">${escapar(evento.descricao)}</p>` : ''}
      </div>
      <div class="d-flex flex-wrap gap-2">
        ${aceitaContribuicoes(evento, agora) ? `<a class="btn btn-sm btn-info" href="#/eventos/${evento.id}/levar">O que eu posso levar?</a>` : ''}
        ${podeEditar(evento) ? `<a class="btn btn-sm btn-outline-primary" href="#/eventos/${evento.id}/editar">Editar</a>` : ''}
        ${evento.status === 'RASCUNHO' ? '<button class="btn btn-sm btn-outline-danger" data-acao="excluir-evento">Excluir</button>' : ''}
        ${botoesStatus}
      </div>
    </div>`;
}

function htmlItens(categorias, itens, editavel) {
  const porCategoria = categorias.map((c) => {
    const linhas = itens.filter((i) => i.categoriaId === c.id).map((i) => `
      <tr>
        <td>${escapar(i.nome)} ${i.obrigatorio ? '<span class="badge text-bg-dark">obrigatório</span>' : ''}</td>
        <td>${i.quantidadeNecessaria} ${escapar(rotulo(UNIDADES, i.unidade))}</td>
        <td>${badgesAlergenos(i.alergenos)}</td>
        <td class="text-end">${editavel ? `<button class="btn btn-sm btn-outline-danger" data-acao="excluir-item" data-id="${i.id}">Remover</button>` : ''}</td>
      </tr>`).join('');
    return `
      <div class="card mb-3">
        <div class="card-header d-flex justify-content-between align-items-center">
          <span><strong>${escapar(c.nome)}</strong> · meta ${c.meta} ${escapar(rotulo(UNIDADES, c.unidade))}</span>
          ${editavel ? `<button class="btn btn-sm btn-outline-danger" data-acao="excluir-categoria" data-id="${c.id}">Remover categoria</button>` : ''}
        </div>
        ${linhas ? `<table class="table mb-0"><tbody>${linhas}</tbody></table>` : '<div class="card-body text-muted">Sem itens.</div>'}
      </div>`;
  }).join('') || '<p class="text-muted">Nenhuma categoria cadastrada.</p>';

  if (!editavel) return porCategoria;
  return `${porCategoria}
    <div class="row g-3">
      <form id="form-categoria" class="col-md-5" novalidate>
        <div class="card card-body">
          <h3 class="h6">Nova categoria</h3>
          <input class="form-control mb-2" name="nome" placeholder="Nome" required>
          <div class="input-group mb-2">
            <input class="form-control" name="meta" type="number" min="1" placeholder="Meta" required>
            <select class="form-select" name="unidade">${opcoes(Object.entries(UNIDADES))}</select>
          </div>
          <button class="btn btn-outline-primary" type="submit">Adicionar categoria</button>
        </div>
      </form>
      ${categorias.length ? `
      <form id="form-item" class="col-md-7" novalidate>
        <div class="card card-body">
          <h3 class="h6">Novo item</h3>
          <div class="input-group mb-2">
            <input class="form-control" name="nome" placeholder="Nome" required>
            <select class="form-select" name="categoriaId">${opcoes(categorias.map((c) => [c.id, c.nome]))}</select>
            <input class="form-control" name="quantidadeNecessaria" type="number" min="1" placeholder="Quantidade" required>
          </div>
          <div class="form-check mb-2">
            <input class="form-check-input" type="checkbox" name="obrigatorio" id="item-obrigatorio">
            <label class="form-check-label" for="item-obrigatorio">Obrigatório</label>
          </div>
          <div class="mb-2">
            ${Object.entries(ALERGENOS).map(([valor, texto]) => `
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" name="alergenos" value="${valor}" id="a-${valor}">
                <label class="form-check-label" for="a-${valor}">${escapar(texto)}</label>
              </div>`).join('')}
          </div>
          <button class="btn btn-outline-primary" type="submit">Adicionar item</button>
        </div>
      </form>` : ''}
    </div>`;
}

function htmlParticipantes(doEvento, todos, aberto) {
  const ids = new Set(doEvento.map((p) => p.id));
  const disponiveis = todos.filter((p) => !ids.has(p.id));
  const lista = doEvento.map((p) => `
    <li class="list-group-item d-flex justify-content-between align-items-center">
      <span>${escapar(p.nome)} ${badgesRestricoes(p.restricoes)}</span>
      ${aberto ? `<button class="btn btn-sm btn-outline-danger" data-acao="sair" data-id="${p.id}">Sair do evento</button>` : ''}
    </li>`).join('');
  return `
    ${lista ? `<ul class="list-group mb-3">${lista}</ul>` : '<p class="text-muted">Ninguém no evento ainda.</p>'}
    ${aberto
      ? `<form id="form-entrar" class="input-group" style="max-width: 32rem">
           <select class="form-select" name="participanteId" required>
             <option value="">Escolha um participante...</option>${opcoes(disponiveis.map((p) => [p.id, p.nome]))}
           </select>
           <button class="btn btn-outline-primary" type="submit">Adicionar ao evento</button>
         </form>`
      : '<p class="text-muted">Participantes só entram ou saem com o evento aberto e antes do prazo.</p>'}`;
}

function htmlContribuicoes(contribuicoes, participantes, cobertura, aberto) {
  const linhas = contribuicoes.map((c) => `
    <tr class="${c.avisos.length ? 'table-warning' : ''}">
      <td>${escapar(c.participanteNome)}</td>
      <td>${escapar(c.itemNome)}</td>
      <td>${c.quantidade} ${escapar(rotulo(UNIDADES, c.unidade))}</td>
      <td>${c.avisos.map((a) => `<div class="small fw-semibold text-danger">⚠ ${escapar(a)}</div>`).join('')}</td>
      <td class="text-end">${aberto ? `<button class="btn btn-sm btn-outline-danger" data-acao="remover-contribuicao" data-id="${c.id}">Remover</button>` : ''}</td>
    </tr>`).join('');
  const pendentes = cobertura.itens.filter((i) => i.faltante > 0);
  return `
    ${linhas
      ? `<table class="table bg-white"><thead><tr><th>Quem</th><th>Item</th><th>Quantidade</th><th>Avisos</th><th></th></tr></thead><tbody>${linhas}</tbody></table>`
      : '<p class="text-muted">Nenhuma contribuição ainda.</p>'}
    ${aberto ? `
      <form id="form-contribuicao" class="card card-body" novalidate>
        <h3 class="h6">Nova contribuição</h3>
        <div class="row g-2">
          <div class="col-md-4">
            <select class="form-select" name="participanteId">
              <option value="">Quem vai levar...</option>${opcoes(participantes.map((p) => [p.id, p.nome]))}
            </select>
            <div class="invalid-feedback" data-erro="participanteId"></div>
          </div>
          <div class="col-md-5">
            <select class="form-select" name="itemId">
              <option value="">O quê...</option>
              ${opcoes(pendentes.map((i) => [i.itemId, `${i.nome} (faltam ${i.faltante} ${rotulo(UNIDADES, i.unidade)})`]))}
            </select>
            <div class="invalid-feedback" data-erro="itemId"></div>
          </div>
          <div class="col-md-2">
            <input class="form-control" name="quantidade" type="number" min="1" placeholder="Qtd.">
            <div class="invalid-feedback" data-erro="quantidade"></div>
          </div>
          <div class="col-md-1 d-grid"><button class="btn btn-primary" type="submit">Incluir</button></div>
        </div>
      </form>` : '<p class="text-muted">Contribuições só podem ser alteradas com o evento aberto e antes do prazo.</p>'}`;
}

export function htmlCobertura(c) {
  const categorias = c.categorias.map((cat) => `
    <div class="mb-2">
      <div class="d-flex justify-content-between small">
        <span>${escapar(cat.nome)}</span>
        <span>${cat.contribuido} de ${cat.meta} ${escapar(rotulo(UNIDADES, cat.unidade))}</span>
      </div>
      ${htmlBarra(cat.percentual)}
    </div>`).join('') || '<p class="text-muted">Sem categorias.</p>';
  const itens = c.itens.map((i) => `
    <tr>
      <td>${escapar(i.nome)} ${i.obrigatorio ? '<span class="badge text-bg-dark">obrigatório</span>' : ''}</td>
      <td>${i.contribuido} de ${i.necessario} ${escapar(rotulo(UNIDADES, i.unidade))}</td>
      <td>${i.faltante > 0 ? `faltam ${i.faltante}` : '<span class="text-success">completo</span>'}</td>
      <td style="min-width: 10rem">${htmlBarra(i.percentual)}</td>
    </tr>`).join('');
  return `
    <div class="card card-body mb-3">
      <div class="d-flex justify-content-between">
        <h3 class="h5">Cobertura geral</h3>
        <span>Obrigatórios completos: <strong>${c.obrigatoriosCompletos} de ${c.obrigatoriosTotal}</strong></span>
      </div>
      ${htmlBarra(c.coberturaGeral)}
    </div>
    <div class="row g-3">
      <div class="col-lg-4"><div class="card card-body"><h3 class="h6">Por categoria</h3>${categorias}</div></div>
      <div class="col-lg-8">
        ${itens ? `<table class="table bg-white"><thead><tr><th>Item</th><th>Contribuído</th><th>Situação</th><th>%</th></tr></thead><tbody>${itens}</tbody></table>` : ''}
      </div>
    </div>`;
}

function htmlRestricoes(r) {
  const resumo = r.resumo.map((x) =>
    `<span class="badge text-bg-info fs-6 me-2 mb-2">${escapar(rotulo(RESTRICOES, x.restricao))}: ${x.participantes}</span>`).join('')
    || '<span class="text-muted">Nenhum participante com restrição.</span>';
  const itens = r.itens.map((i) => `
    <tr class="${i.participantesAfetados ? 'table-warning' : ''}">
      <td>${escapar(i.nome)}</td>
      <td>${badgesAlergenos(i.alergenos)}</td>
      <td>${i.participantesAfetados
        ? `<strong>⚠ ${i.participantesAfetados} participante(s)</strong> ${badgesRestricoes(i.restricoesEmConflito)}`
        : '<span class="text-success">nenhum</span>'}</td>
    </tr>`).join('');
  return `
    <p>${r.participantesAtivos} participante(s) ativo(s).</p>
    <div class="mb-3">${resumo}</div>
    ${itens ? `<table class="table bg-white"><thead><tr><th>Item</th><th>Alérgenos</th><th>Afetados</th></tr></thead><tbody>${itens}</tbody></table>` : ''}`;
}

function marcarErros(form, erros) {
  form.querySelectorAll('[name]').forEach((campo) => campo.classList.toggle('is-invalid', Boolean(erros[campo.name])));
  form.querySelectorAll('[data-erro]').forEach((div) => { div.textContent = erros[div.dataset.erro] ?? ''; });
}

export async function renderDetalheEvento(container, { id }) {
  const agora = new Date();
  const [evento, categorias, itens, doEvento, todos, contribuicoes, cobertura, restricoes] = await Promise.all([
    api.buscarEvento(id), api.listarCategorias(id), api.listarItens(id), api.listarParticipantesDoEvento(id),
    api.listarParticipantes(), api.listarContribuicoes(id), api.cobertura(id), api.restricoes(id),
  ]);
  const editavel = podeEditar(evento);
  const aberto = aceitaContribuicoes(evento, agora);
  const conteudo = {
    itens: htmlItens(categorias, itens, editavel),
    participantes: htmlParticipantes(doEvento, todos, aberto),
    contribuicoes: htmlContribuicoes(contribuicoes, doEvento, cobertura, aberto),
    cobertura: htmlCobertura(cobertura),
    restricoes: htmlRestricoes(restricoes),
  };

  container.innerHTML = `
    <div id="detalhe">
      ${htmlCabecalho(evento, agora)}
      <ul class="nav nav-tabs mb-3" role="tablist">
        ${ABAS.map(([chave, texto]) => `
          <li class="nav-item" role="presentation">
            <button class="nav-link ${chave === abaAtiva ? 'active' : ''}" data-bs-toggle="tab" data-bs-target="#aba-${chave}"
                    data-aba="${chave}" type="button" role="tab">${texto}</button>
          </li>`).join('')}
      </ul>
      <div class="tab-content">
        ${ABAS.map(([chave]) => `
          <div class="tab-pane fade ${chave === abaAtiva ? 'show active' : ''}" id="aba-${chave}" role="tabpanel">${conteudo[chave]}</div>`).join('')}
      </div>
    </div>`;

  const raiz = container.querySelector('#detalhe');
  const recarregar = () => renderDetalheEvento(container, { id });

  // executa a ação, mostra o erro se houver e redesenha a página
  const executar = async (acao) => {
    limparAlertas();
    try {
      const resultado = await acao();
      await recarregar();
      return resultado;
    } catch (erro) {
      mostrarErro(erro);
      return undefined;
    }
  };

  raiz.addEventListener('click', async (e) => {
    const aba = e.target.closest('[data-aba]');
    if (aba) abaAtiva = aba.dataset.aba;

    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    const alvo = botao.dataset.id;
    switch (botao.dataset.acao) {
      case 'status':
        if (botao.dataset.status === 'CANCELADO' && !confirm('Cancelar o evento? Essa ação é final.')) return;
        await executar(() => api.mudarStatus(id, botao.dataset.status));
        break;
      case 'excluir-evento':
        if (!confirm('Excluir o evento?')) return;
        limparAlertas();
        try {
          await api.excluirEvento(id);
          location.hash = '#/eventos';
        } catch (erro) {
          mostrarErro(erro);
        }
        break;
      case 'excluir-categoria':
        await executar(() => api.excluirCategoria(id, alvo));
        break;
      case 'excluir-item':
        await executar(() => api.excluirItem(id, alvo));
        break;
      case 'sair':
        if (!confirm('Tirar este participante do evento? As contribuições dele serão removidas.')) return;
        await executar(() => api.sairDoEvento(id, alvo));
        break;
      case 'remover-contribuicao':
        await executar(() => api.removerContribuicao(id, alvo));
        break;
    }
  });

  raiz.addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const dados = new FormData(form);
    switch (form.id) {
      case 'form-categoria':
        await executar(() => api.criarCategoria(id, {
          nome: dados.get('nome'), meta: Number(dados.get('meta')) || null, unidade: dados.get('unidade'),
        }));
        break;
      case 'form-item':
        await executar(() => api.criarItem(id, {
          nome: dados.get('nome'),
          categoriaId: Number(dados.get('categoriaId')),
          quantidadeNecessaria: Number(dados.get('quantidadeNecessaria')) || null,
          obrigatorio: dados.has('obrigatorio'),
          alergenos: dados.getAll('alergenos'),
        }));
        break;
      case 'form-entrar':
        if (!dados.get('participanteId')) return;
        await executar(() => api.entrarNoEvento(id, dados.get('participanteId')));
        break;
      case 'form-contribuicao': {
        const contribuicao = {
          participanteId: dados.get('participanteId'),
          itemId: dados.get('itemId'),
          quantidade: dados.get('quantidade'),
        };
        const item = cobertura.itens.find((i) => String(i.itemId) === contribuicao.itemId);
        const erros = validarContribuicao(contribuicao, item?.faltante);
        marcarErros(form, erros);
        if (Object.keys(erros).length) return;
        const criada = await executar(() => api.criarContribuicao(id, {
          participanteId: Number(contribuicao.participanteId),
          itemId: Number(contribuicao.itemId),
          quantidade: Number(contribuicao.quantidade),
        }));
        if (criada) {
          mostrarSucesso('Contribuição incluída.');
          mostrarAvisos(criada.avisos);
        }
        break;
      }
    }
  });
}
