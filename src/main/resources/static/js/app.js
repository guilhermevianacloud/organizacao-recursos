// Organização de Recursos — interface web. Fala só com a API (/api/...) usando o token JWT do login.
// Todo texto vindo da API passa por esc() antes de entrar no HTML (evita injeção de HTML).
(function () {
  'use strict';

  const sessao = JSON.parse(sessionStorage.getItem('sessao') || 'null');
  if (!sessao || !sessao.token || new Date(sessao.expiraEm) <= new Date()) {
    sessionStorage.removeItem('sessao');
    location.replace('/');
    return;
  }

  const PERFIL = sessao.perfil;
  const NOMES_PERFIL = { SOLICITANTE: 'Solicitante', RESPONSAVEL: 'Responsável', ADMINISTRADOR: 'Administrador' };
  const NOMES_ESTADO = {
    SOLICITADA: 'Solicitada', APROVADA: 'Aprovada', EM_USO: 'Em uso', CONCLUIDA: 'Concluída',
    REJEITADA: 'Rejeitada', CANCELADA: 'Cancelada', NAO_COMPARECEU: 'Não compareceu',
  };
  const NOMES_TIPO = { SALA: 'Sala', MATERIAL: 'Material', PROFESSOR: 'Professor' };
  const HORA_INICIO = 7;
  const HORA_FIM = 23;

  // Seções por perfil: espelha a matriz de autorização (docs/seguranca/matriz-autorizacao.md).
  const SECOES = [
    { id: 'agenda', titulo: 'Agenda do dia', perfis: ['SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR'], abrir: telaAgenda },
    { id: 'nova', titulo: 'Nova reserva', perfis: ['SOLICITANTE'], abrir: telaNovaReserva },
    { id: 'minhas', titulo: 'Minhas reservas', perfis: ['SOLICITANTE'], abrir: telaMinhas },
    { id: 'aprovacoes', titulo: 'Aprovações', perfis: ['RESPONSAVEL'], abrir: telaAprovacoes },
    { id: 'relatorio', titulo: 'Relatório', perfis: ['RESPONSAVEL', 'ADMINISTRADOR'], abrir: telaRelatorio },
    { id: 'recursos', titulo: 'Recursos', perfis: ['ADMINISTRADOR'], abrir: telaRecursos },
    { id: 'manutencao', titulo: 'Manutenção', perfis: ['ADMINISTRADOR'], abrir: telaManutencao },
  ].filter((s) => s.perfis.includes(PERFIL));

  const conteudo = document.getElementById('conteudo');
  const detalhe = document.getElementById('detalhe');
  let diaDaAgenda = hojeLocal();

  document.getElementById('nome-usuario').textContent = sessao.nome;
  document.getElementById('perfil-usuario').textContent = NOMES_PERFIL[PERFIL] || PERFIL;
  document.getElementById('sair').addEventListener('click', sair);
  window.addEventListener('hashchange', navegar);
  detalhe.addEventListener('click', (e) => { if (e.target === detalhe) fecharDetalhe(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') fecharDetalhe(); });
  navegar();

  // ---------- Utilidades ----------

  function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  async function api(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: { Authorization: 'Bearer ' + sessao.token } };
    if (corpo !== undefined) {
      opcoes.headers['Content-Type'] = 'application/json';
      opcoes.body = JSON.stringify(corpo);
    }
    const resposta = await fetch(caminho, opcoes);
    if (resposta.status === 401) {
      sair();
      throw new Error('Sessão expirada.');
    }
    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
      const erro = new Error((dados && dados.detail) || 'Não foi possível concluir o pedido.');
      erro.codigo = dados && dados.codigo;
      throw erro;
    }
    return dados;
  }

  function aviso(tipo, texto, codigo) {
    return '<div class="aviso ' + tipo + '" role="' + (tipo === 'erro' ? 'alert' : 'status') + '">' + esc(texto)
      + (codigo ? ' <span class="codigo">(' + esc(codigo) + ')</span>' : '') + '</div>';
  }

  // Para telas disparadas sem await (eventos): o erro aparece na tela em vez de virar rejeição sem tratamento.
  function emSegundoPlano(promessa) {
    promessa.catch((e) => { conteudo.innerHTML = aviso('erro', e.message, e.codigo); });
  }

  function etiqueta(estado) {
    return '<span class="estado estado-' + esc(estado) + '">' + esc(NOMES_ESTADO[estado] || estado) + '</span>';
  }

  function hojeLocal() {
    const d = new Date();
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  }

  function paraIso(dia, hora) {
    return new Date(dia + 'T' + hora).toISOString();
  }

  function hora(iso) {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  function dataHora(iso) {
    return new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  function diaSeguinte(dia) {
    const d = new Date(dia + 'T12:00:00');
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  }

  function sair() {
    sessionStorage.removeItem('sessao');
    location.replace('/');
  }

  // ---------- Navegação ----------

  function navegar() {
    const id = (location.hash || '#agenda').slice(1);
    const secao = SECOES.find((s) => s.id === id) || SECOES[0];
    document.getElementById('menu').innerHTML = SECOES.map((s) =>
      '<a href="#' + s.id + '"' + (s.id === secao.id ? ' aria-current="page"' : '') + '>' + esc(s.titulo)
      + (s.id === 'aprovacoes' ? '<span class="contador oculto" id="contador-pendentes"></span>' : '') + '</a>').join('');
    document.title = secao.titulo + ' · Organização de Recursos';
    secao.abrir().catch((e) => { conteudo.innerHTML = aviso('erro', e.message, e.codigo); });
    if (PERFIL === 'RESPONSAVEL') emSegundoPlano(atualizarContadorDePendentes());
  }

  async function atualizarContadorDePendentes() {
    try {
      const pendentes = await api('GET', '/api/reservas/pendentes');
      const contador = document.getElementById('contador-pendentes');
      if (contador && pendentes.length) {
        contador.textContent = pendentes.length;
        contador.classList.remove('oculto');
      }
    } catch (e) { /* o contador é só um atalho */ }
  }

  // ---------- Agenda ----------

  async function telaAgenda() {
    conteudo.innerHTML =
      '<div class="cabecalho"><div><h1>Agenda do dia</h1><p class="mudo">Salas, materiais e professores. Clique numa reserva para ver detalhes.</p></div>'
      + '<label class="rotulo">Dia<input class="campo" type="date" id="dia" value="' + esc(diaDaAgenda) + '"></label></div>'
      + '<section class="painel" aria-label="Linha do tempo"><div id="grade" class="linha-do-tempo"><p class="mudo">Carregando…</p></div>'
      + '<div class="legenda">' + ['APROVADA', 'SOLICITADA', 'EM_USO', 'CONCLUIDA'].map(etiqueta).join('')
      + '<span class="estado bloco manutencao" style="position:static">Manutenção</span></div></section>';
    document.getElementById('dia').addEventListener('change', (e) => { diaDaAgenda = e.target.value; emSegundoPlano(telaAgenda()); });

    const [recursos, reservasHoje, reservasAmanha, manutencoes] = await Promise.all([
      api('GET', '/api/recursos'),
      api('GET', '/api/reservas?data=' + diaDaAgenda),
      api('GET', '/api/reservas?data=' + diaSeguinte(diaDaAgenda)),
      api('GET', '/api/manutencoes'),
    ]);
    const inicioDia = new Date(diaDaAgenda + 'T' + String(HORA_INICIO).padStart(2, '0') + ':00').getTime();
    const fimDia = new Date(diaDaAgenda + 'T00:00').getTime() + HORA_FIM * 3600000;
    const duracao = fimDia - inicioDia;
    const visiveis = (inicio, fim) => new Date(inicio).getTime() < fimDia && inicioDia < new Date(fim).getTime();
    const posicao = (inicio, fim) => {
      const a = Math.max(new Date(inicio).getTime(), inicioDia);
      const b = Math.min(new Date(fim).getTime(), fimDia);
      return 'left:' + ((a - inicioDia) / duracao * 100).toFixed(3) + '%;width:' + ((b - a) / duracao * 100).toFixed(3) + '%';
    };
    const vistas = new Set();
    const reservas = reservasHoje.concat(reservasAmanha).filter((r) => {
      if (vistas.has(r.id)) return false;
      vistas.add(r.id);
      return visiveis(r.inicio, r.termino) && !['CANCELADA', 'REJEITADA'].includes(r.estado);
    });

    let html = '<div class="grade-tempo"><div></div><div class="horas">';
    for (let h = HORA_INICIO; h <= HORA_FIM; h += 2) {
      html += '<span class="hora" style="left:' + ((h - HORA_INICIO) / (HORA_FIM - HORA_INICIO) * 100) + '%">' + String(h).padStart(2, '0') + 'h</span>';
    }
    html += '</div>';
    recursos.forEach((recurso) => {
      html += '<div class="nome-recurso"><span>' + esc(recurso.nome) + '</span><small>' + esc(NOMES_TIPO[recurso.tipo])
        + (recurso.restrito ? ' · restrito' : '') + '</small></div><div class="trilho">';
      manutencoes.filter((m) => m.recursoId === recurso.id && visiveis(m.inicio, m.termino)).forEach((m) => {
        html += '<span class="bloco manutencao" style="' + posicao(m.inicio, m.termino) + '" title="Manutenção: ' + esc(m.motivo) + '">Manutenção</span>';
      });
      reservas.filter((r) => r.recursoId === recurso.id).forEach((r) => {
        html += '<button type="button" class="bloco estado-' + esc(r.estado) + '" style="' + posicao(r.inicio, r.termino)
          + '" data-reserva="' + r.id + '" aria-label="' + esc(recurso.nome + ', ' + hora(r.inicio) + ' às ' + hora(r.termino) + ', ' + NOMES_ESTADO[r.estado]) + '">'
          + esc(hora(r.inicio) + '–' + hora(r.termino) + ' · ' + NOMES_ESTADO[r.estado]) + '</button>';
      });
      html += '</div>';
    });
    html += '</div>';
    const grade = document.getElementById('grade');
    grade.innerHTML = recursos.length ? html : '<p class="vazio">Nenhum recurso cadastrado.</p>';
    grade.querySelectorAll('[data-reserva]').forEach((b) => b.addEventListener('click', () => {
      emSegundoPlano(abrirDetalhe(reservas.find((r) => r.id === Number(b.dataset.reserva))));
    }));
  }

  // ---------- Detalhe da reserva (ações e histórico) ----------

  const ACOES_DO_RESPONSAVEL = {
    SOLICITADA: [['aprovacao', 'Aprovar'], ['rejeicao', 'Rejeitar']],
    APROVADA: [['retirada', 'Registrar retirada'], ['nao-comparecimento', 'Não compareceu']],
    EM_USO: [['devolucao', 'Registrar devolução']],
  };

  async function abrirDetalhe(reserva) {
    const acoes = PERFIL === 'RESPONSAVEL' ? (ACOES_DO_RESPONSAVEL[reserva.estado] || []) : [];
    detalhe.innerHTML = '<section class="painel"><div class="cabecalho"><h2 id="titulo-detalhe">' + esc(reserva.recursoNome) + '</h2>'
      + etiqueta(reserva.estado) + '</div>'
      + '<p>' + esc(dataHora(reserva.inicio) + ' às ' + hora(reserva.termino)) + (reserva.motivo ? '<br><span class="mudo">' + esc(reserva.motivo) + '</span>' : '') + '</p>'
      + '<div id="msg-detalhe"></div>'
      + (acoes.length ? '<div class="item"><div class="acoes">' + acoes.map(([a, t]) =>
        '<button type="button" class="botao pequeno' + (a === 'rejeicao' || a === 'nao-comparecimento' ? ' perigo' : '') + '" data-acao="' + a + '">' + esc(t) + '</button>').join('') + '</div></div>' : '')
      + '<div id="historico"></div>'
      + '<div><button type="button" class="botao secundario" id="fechar">Fechar</button></div></section>';
    detalhe.classList.remove('oculto');
    document.getElementById('fechar').addEventListener('click', fecharDetalhe);
    document.getElementById('fechar').focus();
    detalhe.querySelectorAll('[data-acao]').forEach((b) => b.addEventListener('click', async () => {
      try {
        await api('POST', '/api/reservas/' + reserva.id + '/' + b.dataset.acao);
        fecharDetalhe();
        navegar();
      } catch (e) {
        document.getElementById('msg-detalhe').innerHTML = aviso('erro', e.message, e.codigo);
      }
    }));
    if (PERFIL !== 'SOLICITANTE') {
      try {
        const trilha = await api('GET', '/api/reservas/' + reserva.id + '/historico');
        document.getElementById('historico').innerHTML = '<h3>Histórico</h3><ol class="trilha">' + trilha.map((t) =>
          '<li><strong>' + esc(t.estadoAnterior ? NOMES_ESTADO[t.estadoAnterior] + ' → ' : '') + esc(NOMES_ESTADO[t.estadoNovo]) + '</strong> · '
          + esc(dataHora(t.instante)) + '<br><span class="mudo">' + esc(t.motivo) + ' — ' + esc(t.usuarioNome) + '</span></li>').join('') + '</ol>';
      } catch (e) { /* histórico só para Responsável e Administrador */ }
    }
  }

  function fecharDetalhe() {
    detalhe.classList.add('oculto');
    detalhe.innerHTML = '';
  }

  // ---------- Nova reserva ----------

  async function telaNovaReserva() {
    const recursos = await api('GET', '/api/recursos');
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Nova reserva</h1><p class="mudo">Escolha o horário e veja o que está livre. Recursos restritos aguardam aprovação.</p></div></div>'
      + '<section class="painel"><form id="busca" class="formulario" novalidate><div class="grade-campos">'
      + '<label class="rotulo">Dia<input class="campo" type="date" id="b-dia" value="' + esc(diaDaAgenda) + '" required></label>'
      + '<label class="rotulo">Início<input class="campo" type="time" id="b-inicio" value="08:00" step="300" required></label>'
      + '<label class="rotulo">Término<input class="campo" type="time" id="b-fim" value="10:00" step="300" required></label>'
      + '<label class="rotulo">Tipo<select class="campo" id="b-tipo"><option value="">Todos</option><option value="SALA">Sala</option><option value="MATERIAL">Material</option><option value="PROFESSOR">Professor</option></select></label>'
      + '<label class="rotulo">Capacidade mínima<input class="campo" type="number" min="0" id="b-capacidade"></label>'
      + '<label class="rotulo">Localização<input class="campo" id="b-local" placeholder="ex.: Bloco A"></label>'
      + '</div><label class="rotulo">Motivo (opcional)<input class="campo" id="b-motivo" maxlength="255" placeholder="ex.: Aula de Algoritmos"></label>'
      + '<div><button class="botao" type="submit">Ver recursos livres</button></div></form></section>'
      + '<div id="resultado"></div><section class="painel"><h2>Livres no horário</h2><div id="livres" class="lista"><p class="mudo">Faça uma busca.</p></div></section>';
    if (!recursos.length) document.getElementById('livres').innerHTML = '<p class="vazio">Nenhum recurso cadastrado.</p>';
    document.getElementById('busca').addEventListener('submit', async (e) => {
      e.preventDefault();
      const dia = document.getElementById('b-dia').value;
      const inicio = paraIso(dia, document.getElementById('b-inicio').value);
      const fim = paraIso(dia, document.getElementById('b-fim').value);
      const filtros = new URLSearchParams({ disponivelInicio: inicio, disponivelFim: fim });
      const tipo = document.getElementById('b-tipo').value;
      const capacidade = document.getElementById('b-capacidade').value;
      const local = document.getElementById('b-local').value.trim();
      if (tipo) filtros.set('tipo', tipo);
      if (capacidade) filtros.set('capacidadeMinima', capacidade);
      if (local) filtros.set('localizacao', local);
      const resultado = document.getElementById('resultado');
      resultado.innerHTML = '';
      try {
        const livres = await api('GET', '/api/recursos?' + filtros);
        const lista = document.getElementById('livres');
        lista.innerHTML = livres.length ? livres.map((r) =>
          '<div class="item"><div class="principal"><strong>' + esc(r.nome) + '</strong><br><span class="mudo">'
          + esc([NOMES_TIPO[r.tipo], r.capacidade != null ? r.capacidade + ' lugares' : '', r.localizacao, r.competencias, r.restrito ? 'restrito: precisa de aprovação' : ''].filter(Boolean).join(' · '))
          + '</span></div><div class="acoes"><button class="botao pequeno" type="button" data-reservar="' + r.id + '">Reservar</button></div></div>').join('')
          : '<p class="vazio">Nada livre nesse horário com esses filtros.</p>';
        lista.querySelectorAll('[data-reservar]').forEach((b) => b.addEventListener('click', async () => {
          try {
            const criada = await api('POST', '/api/reservas', {
              recursoId: Number(b.dataset.reservar), inicio, termino: fim,
              motivo: document.getElementById('b-motivo').value.trim() || null,
            });
            resultado.innerHTML = aviso('sucesso', 'Reserva criada: ' + criada.recursoNome + ', ' + dataHora(criada.inicio) + ' às '
              + hora(criada.termino) + ' — ' + NOMES_ESTADO[criada.estado]
              + (criada.estado === 'SOLICITADA' ? ' (aguardando aprovação de um Responsável).' : '.'));
            diaDaAgenda = dia;
            b.closest('.item').remove();
          } catch (erro) {
            resultado.innerHTML = aviso('erro', erro.message, erro.codigo);
          }
        }));
      } catch (erro) {
        resultado.innerHTML = aviso('erro', erro.message, erro.codigo);
      }
    });
  }

  // ---------- Minhas reservas ----------

  async function telaMinhas() {
    const reservas = await api('GET', '/api/reservas/minhas');
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Minhas reservas</h1><p class="mudo">Cancelar não apaga: a reserva fica no histórico.</p></div></div>'
      + '<div id="resultado"></div><section class="painel"><div class="lista">'
      + (reservas.length ? reservas.slice().reverse().map((r) =>
        '<div class="item"><div class="principal"><strong>' + esc(r.recursoNome) + '</strong> ' + etiqueta(r.estado)
        + '<br><span class="mudo">' + esc(dataHora(r.inicio) + ' às ' + hora(r.termino)) + (r.motivo ? ' · ' + esc(r.motivo) : '') + '</span></div>'
        + (['SOLICITADA', 'APROVADA'].includes(r.estado) ? '<div class="acoes"><button class="botao perigo pequeno" type="button" data-cancelar="' + r.id + '">Cancelar</button></div>' : '')
        + '</div>').join('') : '<p class="vazio">Você ainda não tem reservas. Use “Nova reserva”.</p>')
      + '</div></section>';
    conteudo.querySelectorAll('[data-cancelar]').forEach((b) => b.addEventListener('click', async () => {
      if (!confirm('Cancelar esta reserva?')) return;
      try {
        await api('POST', '/api/reservas/' + b.dataset.cancelar + '/cancelamento');
        await telaMinhas();
      } catch (e) {
        document.getElementById('resultado').innerHTML = aviso('erro', e.message, e.codigo);
      }
    }));
  }

  // ---------- Aprovações ----------

  async function telaAprovacoes() {
    const pendentes = await api('GET', '/api/reservas/pendentes');
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Aprovações</h1><p class="mudo">Reservas de recursos restritos aguardando um Responsável.</p></div></div>'
      + '<div id="resultado"></div><section class="painel"><div class="lista">'
      + (pendentes.length ? pendentes.map((r) =>
        '<div class="item"><div class="principal"><strong>' + esc(r.recursoNome) + '</strong> ' + etiqueta(r.estado)
        + '<br><span class="mudo">' + esc(dataHora(r.inicio) + ' às ' + hora(r.termino)) + (r.motivo ? ' · ' + esc(r.motivo) : '') + '</span></div>'
        + '<div class="acoes"><button class="botao pequeno" type="button" data-acao="aprovacao" data-id="' + r.id + '">Aprovar</button>'
        + '<button class="botao perigo pequeno" type="button" data-acao="rejeicao" data-id="' + r.id + '">Rejeitar</button></div></div>').join('')
        : '<p class="vazio">Nenhuma reserva aguardando aprovação.</p>')
      + '</div></section>';
    conteudo.querySelectorAll('[data-acao]').forEach((b) => b.addEventListener('click', async () => {
      try {
        await api('POST', '/api/reservas/' + b.dataset.id + '/' + b.dataset.acao);
        navegar();
      } catch (e) {
        document.getElementById('resultado').innerHTML = aviso('erro', e.message, e.codigo);
      }
    }));
  }

  // ---------- Relatório ----------

  async function telaRelatorio() {
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Relatório de utilização</h1><p class="mudo">Conta só reservas aprovadas, em uso ou concluídas; reserva que cruza a borda conta só as horas dentro do período.</p></div></div>'
      + '<section class="painel"><form id="periodo" class="grade-campos" novalidate>'
      + '<label class="rotulo">De<input class="campo" type="date" id="r-inicio" value="' + esc(diaDaAgenda) + '"></label>'
      + '<label class="rotulo">Até (inclusive)<input class="campo" type="date" id="r-fim" value="' + esc(diaDaAgenda) + '"></label>'
      + '<div style="align-self:end"><button class="botao" type="submit">Gerar</button></div></form></section><div id="relatorio"></div>';
    const gerar = async () => {
      const inicio = new Date(document.getElementById('r-inicio').value + 'T00:00').toISOString();
      const fim = new Date(diaSeguinte(document.getElementById('r-fim').value) + 'T00:00').toISOString();
      const alvo = document.getElementById('relatorio');
      try {
        const r = await api('GET', '/api/relatorios/utilizacao?' + new URLSearchParams({ inicio, fim }));
        const total = r.recursos.reduce((s, u) => s + u.horas, 0);
        const maior = Math.max(1, ...r.recursos.map((u) => u.horas));
        const barras = (lista) => lista.length ? lista.map((u) =>
          '<div class="barra-uso"><span>' + esc(u.nome) + '</span><span class="trilho-barra"><span class="preenchido" style="width:'
          + (u.horas / maior * 100).toFixed(1) + '%;display:block"></span></span><span class="mono">' + u.horas.toLocaleString('pt-BR') + ' h</span></div>').join('')
          : '<p class="vazio">Sem uso no período.</p>';
        alvo.innerHTML = '<div class="metricas"><div class="metrica"><span class="mudo">Horas reservadas</span><strong>' + total.toLocaleString('pt-BR') + ' h</strong></div>'
          + '<div class="metrica"><span class="mudo">Recursos usados</span><strong>' + r.recursos.length + '</strong></div>'
          + '<div class="metrica"><span class="mudo">Conflitos evitados</span><strong>' + r.conflitosEvitados + '</strong></div></div>'
          + '<section class="painel"><h2>Horas por recurso</h2>' + barras(r.recursos) + '</section>'
          + '<section class="painel"><h2>Carga horária por professor</h2>' + barras(r.professores) + '</section>';
      } catch (e) {
        alvo.innerHTML = aviso('erro', e.message, e.codigo);
      }
    };
    document.getElementById('periodo').addEventListener('submit', (e) => { e.preventDefault(); emSegundoPlano(gerar()); });
    emSegundoPlano(gerar());
  }

  // ---------- Recursos (Administrador) ----------

  async function telaRecursos() {
    const recursos = await api('GET', '/api/recursos');
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Recursos</h1><p class="mudo">Salas, materiais e professores que podem ser reservados.</p></div></div>'
      + '<section class="painel"><h2>Cadastrar</h2><form id="novo-recurso" class="formulario" novalidate><div class="grade-campos">'
      + '<label class="rotulo">Tipo<select class="campo" id="n-tipo"><option value="SALA">Sala</option><option value="MATERIAL">Material</option><option value="PROFESSOR">Professor</option></select></label>'
      + '<label class="rotulo">Nome<input class="campo" id="n-nome" required maxlength="255"></label>'
      + '<label class="rotulo">Capacidade<input class="campo" id="n-capacidade" type="number"></label>'
      + '<label class="rotulo">Localização<input class="campo" id="n-local" maxlength="255"></label>'
      + '<label class="rotulo">Competências (professor)<input class="campo" id="n-competencias" maxlength="255"></label>'
      + '</div><label class="caixa"><input type="checkbox" id="n-restrito"> Restrito: exige aprovação de um Responsável</label>'
      + '<div id="resultado"></div><div><button class="botao" type="submit">Cadastrar</button></div></form></section>'
      + '<section class="painel"><h2>Cadastrados</h2><div class="tabela-rolagem"><table><thead><tr><th>Nome</th><th>Tipo</th><th>Capacidade</th><th>Localização</th><th>Restrito</th></tr></thead><tbody>'
      + recursos.map((r) => '<tr><td>' + esc(r.nome) + '</td><td>' + esc(NOMES_TIPO[r.tipo]) + '</td><td>' + esc(r.capacidade ?? '—')
        + '</td><td>' + esc(r.localizacao || r.competencias || '—') + '</td><td>' + (r.restrito ? 'Sim' : 'Não') + '</td></tr>').join('')
      + '</tbody></table></div></section>';
    document.getElementById('novo-recurso').addEventListener('submit', async (e) => {
      e.preventDefault();
      const capacidade = document.getElementById('n-capacidade').value;
      try {
        await api('POST', '/api/recursos', {
          tipo: document.getElementById('n-tipo').value,
          nome: document.getElementById('n-nome').value,
          capacidade: capacidade === '' ? null : Number(capacidade),
          localizacao: document.getElementById('n-local').value.trim() || null,
          competencias: document.getElementById('n-competencias').value.trim() || null,
          restrito: document.getElementById('n-restrito').checked,
        });
        await telaRecursos();
      } catch (erro) {
        document.getElementById('resultado').innerHTML = aviso('erro', erro.message, erro.codigo);
      }
    });
  }

  // ---------- Manutenção (Administrador) ----------

  async function telaManutencao() {
    const [recursos, manutencoes] = await Promise.all([api('GET', '/api/recursos'), api('GET', '/api/manutencoes')]);
    conteudo.innerHTML = '<div class="cabecalho"><div><h1>Manutenção</h1><p class="mudo">Recurso em manutenção não pode ser reservado no período. Não é possível bloquear por cima de reserva ativa.</p></div></div>'
      + '<section class="painel"><h2>Bloquear recurso</h2><form id="nova-manutencao" class="formulario" novalidate><div class="grade-campos">'
      + '<label class="rotulo">Recurso<select class="campo" id="m-recurso">' + recursos.map((r) => '<option value="' + r.id + '">' + esc(r.nome) + '</option>').join('') + '</select></label>'
      + '<label class="rotulo">Dia<input class="campo" type="date" id="m-dia" value="' + esc(diaDaAgenda) + '"></label>'
      + '<label class="rotulo">Início<input class="campo" type="time" id="m-inicio" value="13:00" step="300"></label>'
      + '<label class="rotulo">Término<input class="campo" type="time" id="m-fim" value="17:00" step="300"></label>'
      + '</div><label class="rotulo">Motivo<input class="campo" id="m-motivo" maxlength="255" placeholder="ex.: troca do ar-condicionado"></label>'
      + '<div id="resultado"></div><div><button class="botao" type="submit">Bloquear</button></div></form></section>'
      + '<section class="painel"><h2>Períodos de manutenção</h2><div class="lista">'
      + (manutencoes.length ? manutencoes.map((m) => '<div class="item"><div class="principal"><strong>' + esc(m.recursoNome) + '</strong><br><span class="mudo">'
        + esc(dataHora(m.inicio) + ' às ' + hora(m.termino)) + (m.motivo ? ' · ' + esc(m.motivo) : '') + '</span></div></div>').join('')
        : '<p class="vazio">Nenhum período de manutenção.</p>')
      + '</div></section>';
    document.getElementById('nova-manutencao').addEventListener('submit', async (e) => {
      e.preventDefault();
      const dia = document.getElementById('m-dia').value;
      try {
        await api('POST', '/api/manutencoes', {
          recursoId: Number(document.getElementById('m-recurso').value),
          inicio: paraIso(dia, document.getElementById('m-inicio').value),
          termino: paraIso(dia, document.getElementById('m-fim').value),
          motivo: document.getElementById('m-motivo').value.trim() || null,
        });
        await telaManutencao();
      } catch (erro) {
        document.getElementById('resultado').innerHTML = aviso('erro', erro.message, erro.codigo);
      }
    });
  }
})();
