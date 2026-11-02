# Registro de defeitos

Um arquivo por defeito (`DEF-001-titulo-curto.md`), com: identificador, descrição, severidade e prioridade (com justificativa), requisito afetado na RTM, passos para reproduzir, evidência, análise de causa raiz (5 porquês) e os commits da correção. A convenção `test(regressao): <id>` / `fix(<id>): ...` (Aula 15) vale a partir do DEF-005; DEF-001 e DEF-002 foram corrigidos dentro do ciclo de TDD (`test(red)` / `feat(green)`), e o DEF-004 em `fix:` sem o identificador.

O mesmo formato serve para as issues do GitHub (modelo em `.github/ISSUE_TEMPLATE/defeito.md`). Cada defeito tem uma issue (#11 a #14, #17 e #18) e, no fim do arquivo, a seção "Rastreio", com commit, PR, teste de regressão e execução verde.

| Defeito | Situação | Severidade / prioridade |
|---|---|---|
| [DEF-001 — dupla reserva sob concorrência](DEF-001-dupla-reserva-sob-concorrencia.md) | corrigido | Crítica / P0 |
| [DEF-002 — deadlock na disputa pelo mesmo recurso](DEF-002-deadlock-na-disputa-pelo-mesmo-recurso.md) | corrigido | Alta / P1 |
| [DEF-003 — p95 acima da meta proposta sob carga](DEF-003-p95-acima-da-meta-sob-carga.md) | aberto (aceite do risco aguardando decisão) | Média / P2 |
| [DEF-004 — apontamentos da primeira análise estática](DEF-004-apontamentos-da-primeira-analise-estatica.md) | corrigido (11 de 12; 1 justificado no ADR-007, proposta) | Alta / P1 |
| [DEF-005 — data vazia derruba a agenda e o relatório](DEF-005-data-vazia-derruba-a-tela.md) | corrigido | Média / P1 |
| [DEF-006 — resposta atrasada da agenda apaga a tela aberta depois](DEF-006-agenda-atrasada-apaga-a-tela-nova.md) | corrigido | Baixa / P2 |
