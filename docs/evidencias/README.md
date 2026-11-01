# Pacote de evidências — v1.0.0-rc1

Aula 14 (Prompt 2). Índice do que prova a qualidade da versão avaliada.

> **Situação: aguardando a tag.** A tag anotada `v1.0.0-rc1` é criada no commit avaliado **depois** que os pull requests
> forem integrados na `main` e o CI estiver verde lá. Os campos *pendente* são preenchidos com o commit da tag e a
> execução do CI desse commit; nenhum link aqui aponta para outro commit.

| Evidência | Onde | Situação |
|---|---|---|
| Commit avaliado (tag `v1.0.0-rc1`) | *pendente* | aguardando integração na `main` |
| Execução do CI no commit da tag | *pendente* (Actions) | aguardando a tag |
| Relatório do JaCoCo (artifact `relatorio-de-cobertura` dessa execução) | *pendente* | aguardando a tag |
| Quality Gate do SonarCloud | *pendente* | aguardando a configuração do `SONAR_TOKEN` |
| Resultado de carga | [`docs/performance/resultado-01.md`](../performance/resultado-01.md) + [`perf/resultados/rodada-01.jtl`](../../perf/resultados/rodada-01.jtl) | pronto |
| Matriz de rastreabilidade | [`docs/rtm.md`](../rtm.md) | pronto (9 de 9 críticos cobertos) |
| Registro de defeitos | [`docs/defeitos/`](../defeitos/) | pronto (DEF-001 e DEF-002 corrigidos; DEF-003 aberto) |
| Relatório de qualidade | [`docs/relatorio-qualidade.md`](../relatorio-qualidade.md) | pronto |
| Mutação manual | [`docs/testes/mutacao-manual.md`](../testes/mutacao-manual.md) | pronto (10 de 10) |
| Evidência de TDD | [`docs/testes/evidencia-tdd.md`](../testes/evidencia-tdd.md) | pronto |
| Checklist da entrega em clone limpo | [`docs/entrega/checklist.md`](../entrega/checklist.md) | pronto |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](../decisao-qualidade.md) | rascunho (campos do grupo a confirmar) |
