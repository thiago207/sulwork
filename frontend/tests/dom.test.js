import { beforeEach, describe, expect, it } from 'vitest';
import { clonar, montar, preencher } from '../src/dom.js';
import { marcarErros, mostrarErro, mostrarErroNoForm } from '../src/ui.js';

const pagina = `
  <ul id="lista"></ul>
  <template data-modelo="linha"><li><b data-campo="nome"></b></li></template>`;

describe('templates', () => {
  it('o conteúdo do template não aparece até ser clonado', () => {
    const raiz = montar(pagina);
    expect(raiz.querySelector('b')).toBeNull();

    const linha = clonar(raiz, 'linha');
    expect(linha.tagName).toBe('LI');
  });

  it('preencher usa texto puro, então HTML vindo da API não vira elemento', () => {
    const linha = preencher(clonar(montar(pagina), 'linha'), { nome: '<script>x</script>' });

    expect(linha.querySelector('script')).toBeNull();
    expect(linha.textContent).toBe('<script>x</script>');
  });
});

describe('alertas e erros de formulário', () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="alertas"></div>
      <form><input name="cpf"><div data-erro="cpf"></div><input name="nome"></form>`;
  });

  it('mostra a mensagem e a lista de campos do erro da API', () => {
    mostrarErro({ message: 'Dados inválidos.', campos: [{ campo: 'cpf', mensagem: 'CPF inválido' }] });

    const alerta = document.querySelector('#alertas .alert-danger');
    expect(alerta.textContent).toContain('Dados inválidos.');
    expect(alerta.querySelector('li').textContent).toBe('CPF: CPF inválido');
  });

  it('no formulário, o erro do campo vai para baixo dele e o alerta não repete', () => {
    const form = document.querySelector('form');

    mostrarErroNoForm(form, {
      message: 'Um ou mais campos são inválidos.',
      campos: [{ campo: 'cpf', mensagem: 'CPF inválido' }, { campo: 'quantidadeNecessaria', mensagem: 'Deve ser maior que zero.' }],
    });

    expect(form.elements.cpf.classList).toContain('is-invalid');
    const itens = [...document.querySelectorAll('#alertas li')].map((li) => li.textContent);
    expect(itens).toEqual(['Quantidade necessária: Deve ser maior que zero.']);
  });

  it('marca só os campos com erro e escreve a mensagem embaixo', () => {
    const form = document.querySelector('form');

    marcarErros(form, { cpf: 'CPF inválido' });

    expect(form.elements.cpf.classList).toContain('is-invalid');
    expect(form.elements.nome.classList).not.toContain('is-invalid');
    expect(form.querySelector('[data-erro="cpf"]').textContent).toBe('CPF inválido');
  });
});
