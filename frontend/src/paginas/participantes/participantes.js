import modelo from './participantes.html?raw';
import './participantes.css';
import { api } from '../../api.js';
import { limparAlertas, marcarErros, mostrarErroNoForm, mostrarSucesso, tentar } from '../../ui.js';
import { renderizar } from '../../dom.js';
import { criarPessoa } from '../../componentes/pessoa.js';
import { criarOpcoesChip } from '../../componentes/opcoesChip.js';
import { confirmar } from '../../componentes/confirmacao.js';
import { RESTRICOES, mascararCpfDigitado, normalizar } from '../../logica/rotulos.js';

const BOTOES = [
  { acao: 'editar', titulo: 'Editar participante', icone: 'bi-pencil', classe: 'btn-icone-neutro' },
  { acao: 'excluir', titulo: 'Excluir participante', icone: 'bi-trash3' },
];

export async function renderParticipantes(container) {
  let participantes = await api.listarParticipantes();
  let editando = null;

  renderizar(container, modelo);
  const form = container.querySelector('#form-participante');
  const busca = container.querySelector('#busca');
  criarOpcoesChip(container.querySelector('#opcoes-restricoes'), 'restricoes', RESTRICOES);

  const desenharLista = () => {
    const termo = normalizar(busca.value);
    const visiveis = participantes.filter((p) => normalizar(p.nome).includes(termo));
    container.querySelector('#lista-participantes').replaceChildren(...visiveis.map((p) => {
      const item = criarPessoa(p, BOTOES);
      item.classList.toggle('editando', p.id === editando?.id);
      return item;
    }));
    const vazio = container.querySelector('#sem-participantes');
    vazio.hidden = visiveis.length > 0;
    vazio.textContent = participantes.length ? 'Ninguém com esse nome.' : 'Nenhum participante cadastrado.';
    container.querySelector('#total').textContent = participantes.length;
  };

  const recarregar = async () => {
    participantes = await api.listarParticipantes();
    desenharLista();
  };

  // o mesmo formulário serve para cadastrar e editar
  const modoEdicao = (participante) => {
    editando = participante;
    form.reset();
    marcarErros(form, {});
    container.querySelector('#titulo-form').textContent = participante ? `Editar ${participante.nome}` : 'Novo participante';
    container.querySelector('#botao-form').textContent = participante ? 'Salvar alterações' : 'Cadastrar';
    container.querySelector('#icone-form').className = `bi ${participante ? 'bi-pencil' : 'bi-person-plus'}`;
    container.querySelector('#cancelar-edicao').hidden = !participante;
    container.querySelector('#ajuda-cpf').hidden = !participante;
    form.elements.cpf.placeholder = participante ? participante.cpf : '000.000.000-00';
    if (participante) {
      form.elements.nome.value = participante.nome;
      form.elements.observacaoRestricao.value = participante.observacaoRestricao ?? '';
      form.querySelectorAll('[name="restricoes"]').forEach((caixa) => {
        caixa.checked = participante.restricoes.includes(caixa.value);
      });
      form.scrollIntoView({ behavior: 'smooth', block: 'start' });
      form.elements.nome.focus();
    }
    desenharLista();
  };

  form.elements.cpf.addEventListener('input', () => {
    form.elements.cpf.value = mascararCpfDigitado(form.elements.cpf.value);
  });
  busca.addEventListener('input', desenharLista);
  container.querySelector('#cancelar-edicao').addEventListener('click', () => modoEdicao(null));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const dados = new FormData(form);
    const corpo = {
      nome: dados.get('nome'),
      cpf: dados.get('cpf') || null,
      restricoes: dados.getAll('restricoes'),
      observacaoRestricao: dados.get('observacaoRestricao') || null,
    };
    limparAlertas();
    try {
      if (editando) {
        await api.editarParticipante(editando.id, corpo);
        mostrarSucesso('Participante atualizado.');
      } else {
        await api.criarParticipante(corpo);
        mostrarSucesso('Participante cadastrado.');
      }
      modoEdicao(null);
      await recarregar();
    } catch (erro) {
      mostrarErroNoForm(form, erro);
    }
  });

  container.querySelector('#lista-participantes').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    const participante = participantes.find((p) => String(p.id) === botao.dataset.id);
    if (botao.dataset.acao === 'editar') {
      modoEdicao(participante);
      return;
    }
    const ok = await confirmar({
      titulo: `Excluir ${participante.nome}?`,
      texto: 'Se a pessoa se cadastrar de novo com o mesmo CPF, o cadastro volta.',
      botao: 'Excluir',
    });
    if (!ok) return;
    limparAlertas();
    await tentar(async () => {
      await api.excluirParticipante(participante.id);
      if (editando?.id === participante.id) modoEdicao(null);
      mostrarSucesso('Participante excluído.');
      await recarregar();
    });
  });

  desenharLista();
}
