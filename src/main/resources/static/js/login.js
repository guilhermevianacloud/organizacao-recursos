// Tela de login: envia e-mail e senha para /api/auth/login e guarda o token só nesta aba (sessionStorage).
(function () {
  const formulario = document.getElementById('formulario');
  const mensagem = document.getElementById('mensagem');

  if (sessionStorage.getItem('sessao')) {
    location.replace('/app.html');
    return;
  }

  document.querySelectorAll('[data-email]').forEach((botao) => {
    botao.addEventListener('click', () => {
      document.getElementById('email').value = botao.dataset.email;
      document.getElementById('senha').focus();
    });
  });

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mensagem.classList.add('oculto');
    const corpo = {
      email: document.getElementById('email').value.trim(),
      senha: document.getElementById('senha').value,
    };
    try {
      const resposta = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(corpo),
      });
      const dados = await resposta.json().catch(() => ({}));
      if (!resposta.ok) {
        mensagem.textContent = dados.detail || 'Não foi possível entrar.';
        mensagem.classList.remove('oculto');
        return;
      }
      sessionStorage.setItem('sessao', JSON.stringify(dados));
      location.replace('/app.html');
    } catch (e) {
      mensagem.textContent = 'Sem conexão com o servidor. Tente novamente.';
      mensagem.classList.remove('oculto');
    }
  });
})();
