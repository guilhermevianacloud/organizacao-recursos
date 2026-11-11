# Relatório de qualidade — Organização de Recursos

Aula 13. Cada afirmação traz **evidência** (de onde vem o número), **interpretação** e **limite** (o que ela não prova).
Nenhum número foi estimado: o que não foi medido está marcado **não medido**, com o motivo.

## 1. Identificação
| Item | Valor |
|---|---|
| Repositório | <https://github.com/guilhermevianacloud/organizacao-recursos> |
| Versão avaliada | tag `v1.0.0-rc3`; o commit dela e a execução do CI nesse commit estão em `docs/evidencias/README.md` |
| Números de teste e cobertura deste relatório | CI [run 41475669466](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41475669466), último commit com mudança de código antes da tag (`7a88faf`); depois dele, só documentação |
| Data | 2026-11-02 |
| Integrantes | Guilherme Viana Lopes · Rafael Souza Silva · Gustavo Santana (turma STADSCAS4NB) |
| Ambiente de teste | CI GitHub Actions (`ubuntu-latest`, Java 21 Temurin, PostgreSQL 17 via Testcontainers); local Windows 11, i7-11390H, 15,7 GB |
| Stack | Java 21 · Spring Boot 3.5.16 · PostgreSQL 17 · Flyway · Spring Security (JWT) |

## 2. Escopo avaliado e fora de escopo
- **Avaliado:** os 14 requisitos funcionais obrigatórios, as regras críticas (RN-01 a RN-10), a API REST, a interface web e os
  não funcionais RNF-01 a RNF-14 (`docs/prd.md`).
- **Fora de escopo:** provedor de notificação real (simulado com WireMock), desempenho em produção (não há produção),
  navegadores antigos, integração com sistema acadêmico real (`docs/plano-de-teste.md`, seção 2.2).

## 3. Estratégia de teste por nível
| Nível | Quantidade (CI run 41475669466) | Ferramenta | Exemplos |
|---|---|---|---|
| Unidade | 98 testes | JUnit 5, AssertJ, Mockito (só isolado) | `IntervaloTest`, `EstadoReservaTest` (49 pares), `ReservaTest`, `ServicoDeReservasTest` |
| Integração, API, concorrência, BDD e E2E | 187 testes | Testcontainers (PostgreSQL real), MockMvc, WireMock, Cucumber, Playwright | `DuplaReservaConcorrenciaIT`, `MatrizDeAutorizacaoIT` (88 células), `AuditoriaIT`, `RelatorioDeUtilizacaoIT`, `DocumentacaoDaApiIT`, 18 cenários BDD, `InterfaceE2EIT` (8 testes) |
| Carga | 1 rodada, 18.899 pedidos | JMeter 5.6.3 | `perf/reservas.jmx` |
| Mutação manual | 11 mutações | script + `git restore` | `docs/testes/mutacao-manual.md` |
| Exploratório | uso da interface pelo grupo | navegador | achou o DEF-005 |

Todos os 285 testes automatizados passaram no CI, com 0 falhas e 0 erros.

## 4. Resultados comparados às metas
| Meta (especificação) | Medido | Evidência | Interpretação | Limite |
|---|---|---|---|---|
| **≥ 80% de linhas** | **98,0%** (583 de 595) | JaCoCo no `verify`, artifact `relatorio-de-cobertura` | Quase todo o código de produção roda durante os testes; o gate reprova o build abaixo da meta (provado com meta de 99%) | Cobertura mostra execução, não verificação — por isso a mutação manual. O JavaScript da interface não entra no JaCoCo; ele é exercitado pelo `InterfaceE2EIT` |
| **≥ 70% de branches** | **95,0%** (134 de 141) | idem | Os ramos das regras críticas estão exercitados; os 7 restantes foram analisados e classificados como cosméticos | idem. O SonarCloud mostra **97,4%** de "cobertura": é o mesmo relatório do JaCoCo, mas o Sonar junta linhas e ramos num número só — (583 + 134) ÷ (595 + 141) = 717 ÷ 736 = 97,4%. Separados, o próprio Sonar mostra 98,0% e 95,0% |
| **100% dos requisitos críticos na RTM** | **9 de 9 (100%) cobertos** | `docs/rtm.md` + `docs/testes/mutacao-manual.md` | Cada regra crítica tem teste verde no CI **e** a quebra da regra derruba esse teste (11 de 11 mutações detectadas, inclusive a RN-06 dentro da linha de aprovação de restrito) | A mutação foi manual e escolhida pelo grupo; não substitui uma ferramenta de mutação automática |
| **0 bugs ou vulnerabilidades críticas** | SonarCloud: 1ª análise com **12 apontamentos (2 de severidade alta)**; depois da correção, **Quality Gate aprovado**, 0 bugs e 0 code smells. **1 apontamento mantido** (`java:S4502`, CSRF) com justificativa. Registro de defeitos: 0 críticos abertos | DEF-004; ADR-007; SonarCloud, branch `main` | A análise estática achou o que os testes não viam (forma, não comportamento); 11 de 12 corrigidos | O S4502 foi aceito no SonarCloud em 2026-11-11 (status *Accepted*), com a justificativa do ADR-007: nota de segurança A e 0 vulnerabilidades abertas. A meta fica **atendida**, com esse risco assumido |
| RNF-01/02 (proposta do grupo): p95 ≤ 300 ms | consulta **332 ms**, criação **506 ms** | `perf/resultados/rodada-01.jtl`, `docs/performance/resultado-01.md` | **Não atende** a meta proposta; erro real 0%, só 201/409, integridade mantida | Máquina local com o gerador junto; uma rodada; pior caso deliberado; um único usuário no CSV |
| RNF-03 integridade sob carga | 1 reserva aceita em 5.652 tentativas | consulta ao banco após a rodada | A regra crítica aguentou carga contínua | idem |
| RNF-09 interface de 360 a 1920 px | sem rolagem lateral em **360 px** | `InterfaceE2EIT.telasCabemEmLarguraDeCelularSemRolagemLateral` | A largura mais difícil (celular) passa | 1920 px não é testado automaticamente |

## 5. Defeitos por severidade e situação
| Defeito | Severidade / prioridade | Situação | Teste que prova a correção | Limite |
|---|---|---|---|---|
| DEF-001 dupla reserva sob concorrência | Crítica / P0 | corrigido | `DuplaReservaConcorrenciaIT` (vermelho: 10, 7, 6, 4 e 6 duplicatas; verde depois) | concorrência simulada com 20 pedidos numa máquina; mais instâncias da aplicação dependem da constraint, testada em `ProtecaoNoBancoIT` |
| DEF-002 deadlock na disputa pelo mesmo recurso | Alta / P1 | corrigido | `DuplaReservaConcorrenciaIT` sem "exceções inesperadas" em 3 rodadas | defeito intermitente: ausência em 3 rodadas não prova ausência em todas |
| DEF-003 p95 acima da meta proposta | Média / P2 | **aberto** — aceite do risco aguardando decisão do grupo | — (próximo experimento em `resultado-01.md`) | causa ainda é hipótese |
| DEF-004 apontamentos da 1ª análise estática | Alta / P1 | corrigido (11 de 12); S4502 justificado no ADR-007 e aceito no SonarCloud em 2026-11-11 (status *Accepted*) | Quality Gate do SonarCloud aprovado | a análise estática não vê comportamento |
| DEF-005 data vazia derruba a agenda e o relatório | Média / P1 | corrigido | `InterfaceE2EIT.def005_*` (3 testes) | cobre campos vazios; outras entradas inválidas digitadas à mão dependem do navegador |
| DEF-006 resposta atrasada da agenda apaga a tela aberta depois | Baixa / P2 | corrigido | `InterfaceE2EIT.def006_…` (resposta retida de propósito) | corrigido só na agenda, a única tela em que o atraso gerava erro. As telas que buscam dados antes de desenhar (Nova reserva, Minhas reservas, Aprovações, Recursos, Manutenção) ainda podem, numa troca muito rápida de menu, reaparecer por cima da tela aberta depois, sem erro |

Também foram encontrados e corrigidos problemas **de teste** (não do produto): isolamento do teste de concorrência,
execuções inválidas da mutação manual e testes de interface que agiam antes de a tela terminar de carregar — registrados em
`docs/testes/evidencia-tdd.md`, `docs/testes/mutacao-manual.md` e no commit `d91b6cb`.

## 6. Riscos residuais
| Risco | Impacto | Situação | Limite do que se sabe |
|---|---|---|---|
| Desempenho abaixo da meta proposta no pico (DEF-003) | demora de até ~0,5 s em 5% das criações no pior caso | aberto; risco assumido por Guilherme Viana Lopes e Gustavo Santana (2026-11-11) | medido só em máquina local |
| CSRF desligado (S4502, ADR-007) | se a credencial passar a ir em cookie, o sistema fica exposto a CSRF | risco assumido por Guilherme Viana Lopes e Gustavo Santana (2026-11-11); aceito no SonarCloud em 2026-11-11 (status *Accepted*) | nenhum teste falharia se a credencial fosse para cookie |
| Spring Boot 3.5 fora do suporte gratuito (ADR-001) | vulnerabilidade nova sem correção pública | aceito por Guilherme Viana Lopes (ADR-001) | depende de vulnerabilidades ainda não publicadas |
| Token JWT não revogável antes de expirar (ADR-003) | acesso de usuário desligado por até 60 min | risco assumido por Guilherme Viana Lopes e Gustavo Santana (2026-11-11) | — |
| Cobertura por mutação manual, não automática | mutações não pensadas pelo grupo podem sobreviver | mitigado por 11 mutações nas regras críticas | o conjunto foi escolhido por quem escreveu os testes |

## 7. Recomendação
**Liberar com ressalvas** a versão candidata `v1.0.0-rc3` para avaliação acadêmica: todos os requisitos funcionais
obrigatórios implementados e com teste automatizado; os quatro limitadores de nota tratados com evidência (aplicação executa,
pipeline presente, autorização testada em 88 células, nenhuma dupla reserva observada sob 20 pedidos simultâneos nem sob carga);
metas de cobertura e de rastreabilidade atingidas; Quality Gate do SonarCloud aprovado. **Ressalvas:** um apontamento de
segurança mantido com justificativa e risco assumido pelo grupo (S4502, ADR-007) e desempenho abaixo da meta proposta (DEF-003). **Não** é uma afirmação de prontidão para produção: tudo foi medido em ambiente local e no CI.
A decisão final foi tomada em 2026-11-11; quem assume cada risco está em `docs/decisao-qualidade.md`.
