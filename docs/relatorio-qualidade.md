# Relatório de qualidade — Organização de Recursos

Aula 13. Cada afirmação traz **evidência** (de onde vem o número), **interpretação** e **limite** (o que ela não prova).
Nenhum número foi estimado: o que não foi medido está marcado **não medido**, com o motivo.

## 1. Identificação
| Item | Valor |
|---|---|
| Repositório | <https://github.com/guilhermevianacloud/organizacao-recursos> |
| Commit avaliado | tag `v1.0.0-rc2`, registrada com o commit e a execução do CI em `docs/evidencias/README.md`. Números de teste e cobertura: CI [run 41346988995](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41346988995) (PR #10) |
| Data | 2026-11-01 |
| Integrantes | Guilherme Viana Lopes · Rafael Souza · Gustavo Santana (turma STADSCAS4NB) |
| Ambiente de teste | CI GitHub Actions (`ubuntu-latest`, Java 21 Temurin, PostgreSQL 17 via Testcontainers); local Windows 11, i7-11390H, 15,7 GB |
| Stack | Java 21 · Spring Boot 3.5.16 · PostgreSQL 17 · Flyway · Spring Security (JWT) |

## 2. Escopo avaliado e fora de escopo
- **Avaliado:** os 14 requisitos funcionais obrigatórios, as regras críticas (RN-01 a RN-10), a API REST, a interface web e os
  não funcionais RNF-01 a RNF-14 (`docs/prd.md`).
- **Fora de escopo:** provedor de notificação real (simulado com WireMock), desempenho em produção (não há produção),
  navegadores antigos, integração com sistema acadêmico real (`docs/plano-de-teste.md`, seção 2.2).

## 3. Estratégia de teste por nível
| Nível | Quantidade (CI run 41346988995) | Ferramenta | Exemplos |
|---|---|---|---|
| Unidade | 98 testes | JUnit 5, AssertJ, Mockito (só isolado) | `IntervaloTest`, `EstadoReservaTest` (49 pares), `ReservaTest`, `ServicoDeReservasTest` |
| Integração, API, concorrência, BDD e E2E | 175 testes | Testcontainers (PostgreSQL real), MockMvc, WireMock, Cucumber, Playwright | `DuplaReservaConcorrenciaIT`, `MatrizDeAutorizacaoIT` (88 células), `AuditoriaIT`, `RelatorioDeUtilizacaoIT`, 13 cenários BDD, `InterfaceE2EIT` |
| Carga | 1 rodada, 18.899 pedidos | JMeter 5.6.3 | `perf/reservas.jmx` |
| Mutação manual | 10 mutações | script + `git restore` | `docs/testes/mutacao-manual.md` |

Todos os 273 testes automatizados passaram no CI, com 0 falhas e 0 erros.

## 4. Resultados comparados às metas
| Meta (especificação) | Medido | Evidência | Interpretação | Limite |
|---|---|---|---|---|
| **≥ 80% de linhas** | **98,0%** (582 de 594) | JaCoCo no `verify`, artifact `relatorio-de-cobertura` | Quase todo o código de produção roda durante os testes; o gate reprova o build abaixo da meta (provado com meta de 99%) | Cobertura mostra execução, não verificação — por isso a mutação manual |
| **≥ 70% de branches** | **95,0%** (134 de 141) | idem | Os ramos das regras críticas estão exercitados; os 7 restantes foram analisados e classificados como cosméticos | idem |
| **100% dos requisitos críticos na RTM** | **9 de 9 (100%) cobertos** | `docs/rtm.md` + `docs/testes/mutacao-manual.md` | Cada regra crítica tem teste verde no CI **e** a quebra da regra derruba esse teste (10 de 10 mutações detectadas) | A mutação foi manual e escolhida pelo grupo; não substitui uma ferramenta de mutação automática |
| **0 bugs ou vulnerabilidades críticas** | SonarCloud: 1ª análise com **12 apontamentos (2 de severidade alta)**; depois da correção, **Quality Gate aprovado** no PR #10, com 0 issues novas. **1 apontamento mantido** (`java:S4502`, CSRF) com justificativa. Registro de defeitos: 0 críticos abertos | DEF-004; ADR-007; análise do PR #10 no SonarCloud | A análise estática achou o que os testes não viam (forma, não comportamento); 11 de 12 corrigidos | O S4502 só deixa de contar quando for marcado como revisado no SonarCloud, e a justificativa (ADR-007) ainda é **proposta**. Até lá, a meta fica **parcial** |
| RNF-01/02 (proposta do grupo): p95 ≤ 300 ms | consulta **332 ms**, criação **506 ms** | `perf/resultados/rodada-01.jtl`, `docs/performance/resultado-01.md` | **Não atende** a meta proposta; erro real 0%, só 201/409, integridade mantida | Máquina local com o gerador junto; uma rodada; pior caso deliberado |
| RNF-03 integridade sob carga | 1 reserva aceita em 5.652 tentativas | consulta ao banco após a rodada | A regra crítica aguenta carga contínua | idem |

## 5. Defeitos por severidade e situação
| Defeito | Severidade / prioridade | Situação | Teste que prova a correção |
|---|---|---|---|
| DEF-001 dupla reserva sob concorrência | Crítica / P0 | corrigido | `DuplaReservaConcorrenciaIT` (vermelho: 10, 7, 6, 4 e 6 duplicatas; verde depois) |
| DEF-002 deadlock na disputa pelo mesmo recurso | Alta / P1 | corrigido | `DuplaReservaConcorrenciaIT` sem "exceções inesperadas" em 3 rodadas |
| DEF-003 p95 acima da meta proposta | Média / P2 | **aberto** — aceite do risco aguardando decisão do grupo | — (próximo experimento em `resultado-01.md`) |
| DEF-004 apontamentos da 1ª análise estática | Alta / P1 | corrigido (11 de 12); S4502 justificado no ADR-007 (proposta) | Quality Gate do SonarCloud aprovado no PR #10 |

Também foram encontrados e corrigidos dois problemas **de teste** (não do produto): isolamento do teste de concorrência e
execuções inválidas da mutação manual — registrados em `docs/testes/evidencia-tdd.md` e `docs/testes/mutacao-manual.md`.

## 6. Riscos residuais
| Risco | Impacto | Situação |
|---|---|---|
| Desempenho abaixo da meta proposta no pico (DEF-003) | demora de até ~0,5 s em 5% das criações no pior caso | aberto; aceite aguardando decisão |
| CSRF desligado (S4502, ADR-007) | se a credencial passar a ir em cookie, o sistema fica exposto a CSRF | justificado; revisão no SonarCloud aguardando o Guilherme |
| Spring Boot 3.5 fora do suporte gratuito (ADR-001) | vulnerabilidade nova sem correção pública | aceito na decisão do ADR-001 |
| Token JWT não revogável antes de expirar (ADR-003) | acesso de usuário desligado por até 60 min | aceito no ADR-003 |
| Cobertura por mutação manual, não automática | mutações não pensadas pelo grupo podem sobreviver | mitigado por 10 mutações nas regras críticas |

## 7. Recomendação
**Liberar com ressalvas** a versão candidata `v1.0.0-rc2` para avaliação acadêmica: todos os requisitos funcionais
obrigatórios implementados e testados; os quatro limitadores de nota eliminados com evidência (aplicação executa,
pipeline presente, autorização testada em 88 células, dupla reserva impossível sob 20 pedidos simultâneos e sob carga);
metas de cobertura e de rastreabilidade atingidas; Quality Gate do SonarCloud aprovado. **Ressalvas:** um apontamento de
segurança mantido com justificativa ainda em proposta (S4502, ADR-007) e desempenho abaixo da meta proposta (DEF-003). **Não** é uma afirmação de prontidão para produção: tudo foi medido em ambiente local e no CI.
A decisão final, com quem aceita cada risco, fica em `docs/decisao-qualidade.md`.
