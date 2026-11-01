# Pacote de evidências — v1.0.0-rc1

Aula 14 (Prompt 2). Índice do que prova a qualidade da versão avaliada.

> **Situação: tag criada em 2026-11-01.** A tag anotada `v1.0.0-rc1` aponta para o commit `ba66400` da `main`, criado
> pela integração dos pull requests #1 a #8, com CI verde nesse mesmo commit. Os links abaixo apontam para esse commit e
> para essa execução. Commits posteriores, como o que atualiza este índice, não fazem parte da versão avaliada.

| Evidência | Onde | Situação |
|---|---|---|
| Commit avaliado (tag `v1.0.0-rc1`) | [`ba66400`](https://github.com/guilhermevianacloud/organizacao-recursos/tree/v1.0.0-rc1) | pronto |
| Execução do CI no commit da tag | [execução 41341089727](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41341089727) (workflow `qualidade`, job `test`) | pronto (sucesso) |
| Relatório do JaCoCo (artifact `relatorio-de-cobertura` dessa execução) | [artifacts da execução 41341089727](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41341089727#artifacts) | pronto |
| Quality Gate do SonarCloud | *pendente* | aguardando a configuração do `SONAR_TOKEN` |
| Resultado de carga | [`docs/performance/resultado-01.md`](../performance/resultado-01.md) + [`perf/resultados/rodada-01.jtl`](../../perf/resultados/rodada-01.jtl) | pronto |
| Matriz de rastreabilidade | [`docs/rtm.md`](../rtm.md) | pronto (9 de 9 críticos cobertos) |
| Registro de defeitos | [`docs/defeitos/`](../defeitos/) | pronto (DEF-001 e DEF-002 corrigidos; DEF-003 aberto) |
| Relatório de qualidade | [`docs/relatorio-qualidade.md`](../relatorio-qualidade.md) | pronto |
| Mutação manual | [`docs/testes/mutacao-manual.md`](../testes/mutacao-manual.md) | pronto (10 de 10) |
| Evidência de TDD | [`docs/testes/evidencia-tdd.md`](../testes/evidencia-tdd.md) | pronto |
| Checklist da entrega em clone limpo | [`docs/entrega/checklist.md`](../entrega/checklist.md) | pronto |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](../decisao-qualidade.md) | rascunho (campos do grupo a confirmar) |
