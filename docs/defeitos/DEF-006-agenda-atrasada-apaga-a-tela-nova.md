# DEF-006 — Resposta atrasada da agenda apaga a tela aberta depois

| Campo | Valor |
|---|---|
| Identificador | DEF-006 |
| Situação | corrigido |
| Requisito afetado | RF-13 / RN-12 (interface e mensagens compreensíveis) |
| Encontrado por | `e2e/InterfaceE2EIT`, durante a correção do DEF-005 (teste que abria o Relatório logo depois do login falhou de forma intermitente) |
| Data | 2026-11-02 |

## Resumo observável
Logo depois do login, a Agenda do dia começa a carregar. Se o usuário abre outra tela antes de a agenda terminar, a resposta atrasada da agenda gera um erro, e o aviso do erro apaga a tela que o usuário abriu.

## Ambiente / versão
`v1.0.0-rc2` (`ae9d613`), Chromium (Playwright) e aplicação inteira com PostgreSQL real (Testcontainers).

## Pré-condições
Usuário logado; a agenda ainda carregando (aparece "Carregando…").

## Passos mínimos para reproduzir
1. Entrar como Administrador.
2. Antes de a agenda terminar de carregar, clicar em "Relatório".

## Resultado obtido
Quando a agenda termina de carregar, ela procura a própria grade (`#grade`), que não existe mais. O erro resultante troca o Relatório por um aviso de erro.

## Resultado esperado / requisito
A tela aberta por último continua na tela; a resposta da agenda é descartada.

## Evidências (sanitizadas)
Falha intermitente no teste `def005_relatorioEManutencaoSemDataMostramMensagemCompreensivel` durante a correção do DEF-005. Reprodução determinística em `def006_trocarDeTelaAntesDaAgendaCarregarMantemATelaNova`: a resposta de `/api/manutencoes` fica retida até o Relatório abrir.

## Frequência, alcance e impacto
Depende do tempo de resposta. É raro com o servidor local e mais provável numa rede lenta. A tela fica perdida até o usuário clicar no menu de novo. Nenhum dado é afetado.

Alcance conferido no código: só a agenda escrevia num elemento que podia ter sumido, e por isso só ela gerava erro. As telas que buscam dados antes de desenhar (Nova reserva, Minhas reservas, Aprovações, Recursos, Manutenção) trocam o conteúdo inteiro: numa troca muito rápida de menu, a tela anterior pode reaparecer por cima da nova, **sem erro**. Isso não foi corrigido nesta versão e fica registrado como limitação.

## Severidade (com justificativa)
**Baixa.** É intermitente, não afeta dados e a recuperação é um clique.

## Prioridade (com justificativa)
**P2.** Pode aparecer numa demonstração; a correção é uma linha e foi feita junto com o DEF-005.

## Causa raiz (5 porquês)
1. Por que a tela nova some? Porque o aviso de erro substitui todo o conteúdo.
2. Por que houve erro? Porque a agenda, ao terminar de carregar, procurou `#grade` e recebeu `null`.
3. Por que `#grade` não existia? Porque outra tela já tinha substituído o conteúdo.
4. Por que a agenda não percebeu? Porque ela buscava a grade **depois** de esperar as respostas e não conferia se ainda estava na tela.
5. Condição controlável: **tela assíncrona que escreve no documento sem conferir se ainda é a tela ativa.**

## Correção
A agenda guarda a referência da própria grade antes de esperar as respostas. Depois, se a grade não estiver mais no documento (`isConnected`), a agenda não desenha nada. Isso também evita que a resposta de um dia anterior desenhe por cima do dia novo, quando o usuário troca de dia rapidamente.

## Critério de verificação
`def006_trocarDeTelaAntesDaAgendaCarregarMantemATelaNova` vermelho sem a correção e verde com ela; `./mvnw -B verify` verde.

## Rastreio (Aula 05: issue, commit, PR e teste de regressão)
| Item | Valor |
|---|---|
| Issue | [#18](https://github.com/guilhermevianacloud/organizacao-recursos/issues/18) |
| Commit do teste de regressão | `d91b6cb` |
| Commit da correção | `478ceaf` fix(DEF-005, DEF-006) |
| Pull request | [#19](https://github.com/guilhermevianacloud/organizacao-recursos/pull/19) |
| Teste de regressão | `e2e/InterfaceE2EIT.def006_trocarDeTelaAntesDaAgendaCarregarMantemATelaNova` |
