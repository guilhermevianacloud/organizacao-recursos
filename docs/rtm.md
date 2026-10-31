# Matriz de Rastreabilidade (RTM)

Liga **requisito → risco → teste → evidência** (especificação do projeto). Cada linha deve abrir um arquivo que existe e apontar uma execução real do CI. "Testado" sem execução não conta.
Meta: **100% dos requisitos críticos com evidência** do CI no commit avaliado.

Situação em 2026-10-31 (Fase 2): requisitos, regras e riscos mapeados; **testes ainda não escritos**, exceto o marco zero.

**Legenda de status:** pendente · vermelho (teste escrito, falhando) · verde (teste passando no CI) · coberto (verde + mutação manual confirmou que o teste detecta a quebra).

## Requisitos críticos
| Requisito / regra | Risco | Teste (caminho e nome) | Nível | Evidência (run do CI) | Status |
|---|---|---|---|---|---|
| RF-06 / RN-03 — dupla reserva | RP-01 | — | concorrência | — | pendente |
| RF-05 / RN-02 — sobreposição (sala, material, professor) | RP-03 | — | unidade + integração | — | pendente |
| RN-01 — término após início | RP-03 | — | unidade + API | — | pendente |
| RF-08 / RN-04 — manutenção | RP-03 | — | unidade + BDD | — | pendente |
| RF-07 / RN-05, RN-06 — aprovação de restrito | RP-02 | — | unidade + BDD + API | — | pendente |
| RN-07 — máquina de estados | — | — | unidade parametrizada | — | pendente |
| RN-08 — reserva iniciada não é apagada | — | — | unidade + API | — | pendente |
| RF-10 / RN-09 — auditoria | RP-04 | — | integração | — | pendente |
| RF-01 / RN-10 — autorização e posse | RP-02 | — | API parametrizada | — | pendente |

## Demais requisitos
| Requisito / regra | Risco | Teste | Nível | Evidência | Status |
|---|---|---|---|---|---|
| RF-02 — cadastros | — | — | API | — | pendente |
| RF-03 — pesquisa | — | — | API | — | pendente |
| RF-04 — criar, alterar, cancelar | RP-02 | — | API | — | pendente |
| RF-09 — retirada e devolução | — | — | API | — | pendente |
| RF-11 / RN-13 — notificação | RP-06 | — | WireMock | — | pendente |
| RF-12 / RN-11 — relatório de utilização | — | — | API caixa-preta | — | pendente |
| RF-13 / RN-12 — interface e mensagens | RP-05 | — | API + end-to-end | — | pendente |
| RF-14 — documentação da API | — | — | revisão | — | pendente |

## Não funcionais
| RNF | Medida | Teste / instrumento | Evidência | Status |
|---|---|---|---|---|
| RNF-01, RNF-02 — desempenho | p95 ≤ 300 ms, 50 usuários | JMeter (`perf/`) | — | pendente |
| RNF-03 — integridade | 1 aceito em 20, 5 de 5 | teste de concorrência | — | pendente |
| RNF-04 — autorização | 100% da matriz | teste parametrizado | — | pendente |
| RNF-06 — erro neutro | 0 vazamentos | testes de API | — | pendente |
| RNF-07 — falha externa | reserva mantida | WireMock | — | pendente |
| RNF-08 — auditoria | 0 órfãos | integração com rollback | — | pendente |
| RNF-10 — cobertura e análise | 80/70; 0 críticos | JaCoCo + SonarCloud | — | pendente |
| RNF-12 — instalabilidade | 1 comando em máquina limpa | `OrganizacaoRecursosApplicationTests` (`src/test/java/br/senac/organizacaorecursos/`) + CI | [run 41207988750](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41207988750) | verde |

## Percentual de requisitos críticos com evidência
**0 de 9 (0%)** — atualizar a cada fase.
