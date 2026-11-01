# Registro de defeitos

Um arquivo por defeito (`DEF-001-titulo-curto.md`), com: identificador, descrição, severidade e prioridade (com justificativa), requisito afetado na RTM, passos para reproduzir, evidência, análise de causa raiz (5 porquês) e os commits da correção (`test(regressao): <id>` e `fix(<id>): ...`).

O mesmo formato serve para as issues do GitHub (modelo em `.github/ISSUE_TEMPLATE/defeito.md`).

| Defeito | Situação | Severidade / prioridade |
|---|---|---|
| [DEF-001 — dupla reserva sob concorrência](DEF-001-dupla-reserva-sob-concorrencia.md) | corrigido | Crítica / P0 |
| [DEF-002 — deadlock na disputa pelo mesmo recurso](DEF-002-deadlock-na-disputa-pelo-mesmo-recurso.md) | corrigido | Alta / P1 |
| [DEF-003 — p95 acima da meta proposta sob carga](DEF-003-p95-acima-da-meta-sob-carga.md) | aberto (aceite do risco aguardando decisão) | Média / P2 |
| [DEF-004 — apontamentos da primeira análise estática](DEF-004-apontamentos-da-primeira-analise-estatica.md) | corrigido (11 de 12; 1 justificado no ADR-007, proposta) | Alta / P1 |
