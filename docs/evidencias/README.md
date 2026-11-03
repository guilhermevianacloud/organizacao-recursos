# Pacote de evidências — v1.0.0-rc2

Aula 14 (Prompt 2). Índice do que prova a qualidade da versão avaliada.

> **Versão avaliada: tag `v1.0.0-rc2`**, criada em 2026-11-02 no commit `8be1deb` da `main`, com CI verde nesse mesmo
> commit. Os links de documento abaixo abrem a versão **da tag** (`/blob/v1.0.0-rc2/...`), não a `main`. Commits
> posteriores, como o que atualiza este índice, não fazem parte da versão avaliada.
>
> A `v1.0.0-rc1` (`ba66400`, CI 41341089727) foi a primeira candidata. Ela continua publicada e não foi movida.
> Foi substituída porque não tinha as correções do SonarCloud (DEF-004), o ciclo ATAM 01 nem a arquitetura atualizada.

| Evidência | Onde | Situação |
|---|---|---|
| Commit avaliado (tag `v1.0.0-rc2`) | [`8be1deb`](https://github.com/guilhermevianacloud/organizacao-recursos/tree/v1.0.0-rc2) | pronto |
| Execução do CI no commit da tag | [execução 41377043205](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41377043205) (workflow `qualidade`, job `test`): unidade 98, integração 175, 0 falhas, gate do JaCoCo atendido | pronto (sucesso) |
| Relatório do JaCoCo (artifact `relatorio-de-cobertura` dessa execução) | [artifacts da execução 41377043205](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41377043205#artifacts): 98,0% linhas, 95,0% ramos | pronto |
| Quality Gate do SonarCloud | [projeto no SonarCloud, branch `main`](https://sonarcloud.io/project/overview?id=guilhermevianacloud_organizacao-recursos&branch=main), análise de 2026-11-02 02:20 UTC no commit `8be1deb`: **aprovado**; 0 bugs, 0 code smells, 0,0% de duplicação; **1 vulnerabilidade** (S4502, CSRF), justificada no ADR-007 | pronto; S4502 aguardando revisão no site |
| Resultado de carga | [`docs/performance/resultado-01.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/performance/resultado-01.md) + [`perf/resultados/rodada-01.jtl`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/perf/resultados/rodada-01.jtl) | pronto (não atende a meta proposta: DEF-003) |
| Matriz de rastreabilidade | [`docs/rtm.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/rtm.md) | pronto (9 de 9 críticos cobertos) |
| Avaliação arquitetural | [`docs/avaliacao-arquitetural/ciclo-01.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/avaliacao-arquitetural/ciclo-01.md) + [`docs/arquitetura.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/arquitetura.md) | pronto |
| Registro de defeitos | [`docs/defeitos/`](https://github.com/guilhermevianacloud/organizacao-recursos/tree/v1.0.0-rc2/docs/defeitos) + [issues #11 a #14](https://github.com/guilhermevianacloud/organizacao-recursos/issues?q=label%3Abug) | pronto (DEF-001, DEF-002 e DEF-004 corrigidos; DEF-003 aberto) |
| Relatório de qualidade | [`docs/relatorio-qualidade.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/relatorio-qualidade.md) | pronto |
| Mutação manual | [`docs/testes/mutacao-manual.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/testes/mutacao-manual.md) | pronto (10 de 10) |
| Evidência de TDD | [`docs/testes/evidencia-tdd.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/testes/evidencia-tdd.md) | pronto |
| Checklist da entrega em clone limpo | [`docs/entrega/checklist.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/entrega/checklist.md) | pronto |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc2/docs/decisao-qualidade.md) | rascunho (campos do grupo a confirmar) |
