# Pacote de evidências — v1.0.0-rc3

Aula 14 (Prompt 2). Índice do que prova a qualidade da versão avaliada.

> **Versão avaliada: tag `v1.0.0-rc3`**, criada em 2026-11-11 no commit `e4e92c0` da `main` (merge do PR #25). Esse
> commit tem exatamente os mesmos arquivos do commit `667f9a3`, testado pelo CI com sucesso na execução 43087335438;
> a execução no próprio commit do merge ficou presa na fila do GitHub. Os links de documento abrem a versão **da tag**
> (`/blob/v1.0.0-rc3/...`), não a `main`; commits posteriores, como o que completa este índice, não fazem parte da
> versão avaliada.
>
> Em relação à `v1.0.0-rc2`, a rc3 traz as correções DEF-005 e DEF-006 (com testes de regressão de interface), os
> testes novos (RF-14, token vencido, transições alternativas, mutação M11), a revisão independente dos documentos, a
> nova identidade visual, a decisão de qualidade com os riscos assumidos e o registro de quem fez o quê.

## Histórico reescrito em 2026-11-11 (autoria)
O grupo trabalhou numa única conta do GitHub. Em 2026-11-11 o histórico da `main` foi reescrito só para registrar o
autor e os coautores de cada commit (`docs/decisao-qualidade.md`); nenhum arquivo mudou. Por isso todos os códigos de
commit mudaram, e os documentos já citam os novos. As execuções do CI anteriores a essa data continuam no GitHub, mas
mostram o código antigo do mesmo commit:

| Referência | Código antigo (nas execuções do CI) | Código atual |
|---|---|---|
| tag `v1.0.0-rc1` | `ba66400` (execução 41341089727) | `89746c4` |
| tag `v1.0.0-rc2` | `8be1deb` (execução 41377043205) | `ae9d613` |
| `main` antes da rc3 | `f8df44d` | `440ca89` (execução 43085064962, verde) |

Os artifacts (relatórios para download) das execuções da rc1 e da rc2 já expiraram; os da rc3 ficam disponíveis por
90 dias.

## Evidências
| Evidência | Onde | Situação |
|---|---|---|
| Commit avaliado (tag `v1.0.0-rc3`) | [`e4e92c0`](https://github.com/guilhermevianacloud/organizacao-recursos/tree/v1.0.0-rc3) — mesma árvore de arquivos (`0f2aa6a`) do `667f9a3` | pronto |
| Execução do CI no commit da tag | [execução 43087335438](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/43087335438) (workflow `qualidade`, job `test`, no `667f9a3`, mesmos arquivos da tag): unidade 98, integração 187, 0 falhas, gate do JaCoCo atendido, análise do SonarCloud | pronto (sucesso) |
| Relatório do JaCoCo (artifact `relatorio-de-cobertura`) | [artifacts da execução 43087335438](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/43087335438#artifacts) (disponíveis até 2027-02-09): 98,0% linhas (583 de 595), 95,0% ramos (134 de 141) | pronto |
| Quality Gate do SonarCloud | [projeto no SonarCloud, branch `main`](https://sonarcloud.io/project/overview?id=guilhermevianacloud_organizacao-recursos&branch=main): **aprovado**; 0 bugs, 0 code smells; **0 vulnerabilidades abertas, nota de segurança A**; o S4502 (CSRF) foi aceito no SonarCloud em 2026-11-11 (status *Accepted*), com a justificativa do [ADR-007](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/adr/ADR-007-csrf-desligado.md), risco assumido pelo grupo. O Sonar mostra 97,4% de cobertura porque junta linhas e ramos num número só (ver `docs/relatorio-qualidade.md`, seção 4) | pronto |
| Resultado de carga | [`docs/performance/resultado-01.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/performance/resultado-01.md) + [`perf/resultados/rodada-01.jtl`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/perf/resultados/rodada-01.jtl) | pronto (não atende a meta proposta: DEF-003, risco assumido) |
| Matriz de rastreabilidade | [`docs/rtm.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/rtm.md) | pronto (9 de 9 críticos cobertos) |
| Avaliação arquitetural | [`docs/avaliacao-arquitetural/ciclo-01.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/avaliacao-arquitetural/ciclo-01.md) + [`docs/arquitetura.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/arquitetura.md) | pronto |
| Registro de defeitos | [`docs/defeitos/`](https://github.com/guilhermevianacloud/organizacao-recursos/tree/v1.0.0-rc3/docs/defeitos) + [issues com o rótulo `bug`](https://github.com/guilhermevianacloud/organizacao-recursos/issues?q=label%3Abug) | pronto (DEF-001, 002, 004, 005 e 006 corrigidos; DEF-003 aberto, risco assumido) |
| Relatório de qualidade | [`docs/relatorio-qualidade.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/relatorio-qualidade.md) | pronto (285 testes: 98 de unidade + 187 de integração) |
| Mutação manual | [`docs/testes/mutacao-manual.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/testes/mutacao-manual.md) | pronto (11 de 11 detectadas) |
| Evidência de TDD | [`docs/testes/evidencia-tdd.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/testes/evidencia-tdd.md) | pronto |
| Checklist da entrega em clone limpo | [`docs/entrega/checklist.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/entrega/checklist.md) | pronto (repetido em 2026-11-09) |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](https://github.com/guilhermevianacloud/organizacao-recursos/blob/v1.0.0-rc3/docs/decisao-qualidade.md) | pronto (riscos assumidos por Guilherme Viana Lopes e Gustavo Santana em 2026-11-11) |
