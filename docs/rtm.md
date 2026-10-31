# Matriz de Rastreabilidade (RTM)

Liga **requisito → risco → teste → evidência** (especificação do projeto). Cada linha deve abrir um arquivo que existe e apontar uma execução real do CI. "Testado" sem execução não conta.
Meta: **100% dos requisitos críticos com evidência** do CI no commit avaliado.

Situação em 2026-10-31 (Fase 3): núcleo de reservas testado; autorização, auditoria e API nas próximas fases.

**Legenda de status:** pendente · vermelho (teste escrito, falhando) · verde (teste passando no CI) · coberto (verde + mutação manual confirmou que o teste detecta a quebra).

## Requisitos críticos
| Requisito / regra | Risco | Teste (caminho e nome) | Nível | Evidência (run do CI) | Status |
|---|---|---|---|---|---|
| RF-06 / RN-03 — dupla reserva | RP-01 · DEF-001, DEF-002 | `servico/DuplaReservaConcorrenciaIT` (5 repetições + professor + controle); `servico/ProtecaoNoBancoIT` | concorrência + integração | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | verde (serviço; 409 pela API na Fase 4) |
| RF-05 / RN-02 — sobreposição (sala, material, professor) | RP-03 | `dominio/IntervaloTest` (`Sobreposicao`); `servico/ServicoDeReservasIT` (`Sobreposicao`) | unidade + integração | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | verde |
| RN-01 — término após início | RP-03 | `dominio/IntervaloTest` (`TerminoPosteriorAoInicio`); `servico/ProtecaoNoBancoIT.bancoRecusaTerminoAntesDoInicio` | unidade + integração | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | verde (400 pela API na Fase 4) |
| RF-08 / RN-04 — manutenção | RP-03 | `servico/ServicoDeReservasIT` (`EmManutencao`) | integração | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | verde (BDD na Fase 5) |
| RF-07 / RN-05, RN-06 — aprovação de restrito | RP-02 | `dominio/ReservaTest` (restrito nasce SOLICITADA; não restrito aprovado) | unidade | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | parcial — quem aprova (perfil) na Fase 4/5 |
| RN-07 — máquina de estados | — | `dominio/EstadoReservaTest` (49 pares); `dominio/ReservaTest` | unidade parametrizada | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | verde |
| RN-08 — reserva iniciada não é apagada | — | `dominio/EstadoReservaTest`, `dominio/ReservaTest.rn08_reservaIniciadaNaoPodeSerApagada` | unidade | [41210306850](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41210306850) | parcial — API na Fase 4 |
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
**5 de 9 (56%) verdes com evidência do CI** (RN-01, RN-02, RN-03, RN-04, RN-07); 2 parciais (RN-05/06, RN-08); 2 pendentes (auditoria, autorização). Nenhum ainda "coberto": a mutação manual é da Fase 7. Caminhos relativos a `src/test/java/br/senac/organizacaorecursos/`. Atualizado em 2026-10-31 (Fase 3).
