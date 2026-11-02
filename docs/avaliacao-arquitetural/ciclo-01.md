# Avaliação arquitetural — ciclo 01

Aulas 06 (Prompt 3) e 07: primeiro ciclo ATAM, comparando a arquitetura **proposta** em `docs/arquitetura.md`
(2026-10-31) com o **código construído** e as medições.

> **Quando foi feito:** 2026-11-01, com o código pronto (`main` em `16adf58`), e não na semana da Aula 06. Por isso este
> ciclo já usa as medições finais (JaCoCo, SonarCloud, JMeter) e funciona como avaliação da versão candidata.
> As divergências da seção 6 foram corrigidas em `docs/arquitetura.md` no mesmo pull request.

Evidência de execução: [CI run 41348450251](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41348450251)
(commit `16adf58`: unidade 98, integração 175, 0 falhas, gate do JaCoCo atendido, análise do SonarCloud enviada).

## 1. Direcionadores
| ID | Direcionador | Origem |
|---|---|---|
| DR-01 | Nenhum recurso alocado duas vezes no mesmo horário | Especificação: objetivo, regras críticas, limitador "dupla reserva" |
| DR-02 | Só quem tem o perfil certo executa cada operação | RF-01; limitador "falha de autorização" |
| DR-03 | Toda mudança rastreável a quem e quando | RF-10; regra de auditoria |
| DR-04 | Qualquer avaliador sobe e testa o sistema só com o README | limitador "aplicação que não executa"; Aula 14 |
| DR-05 | Consulta de disponibilidade rápida no pico | Aula 10; catálogo Len Bass, seção 8 |

## 2. Cenários (seis partes) com o resultado medido
| ID | Fonte | Estímulo | Ambiente | Artefato | Resposta | Medida | Medido | Situação |
|---|---|---|---|---|---|---|---|---|
| CEN-01 | 20 solicitantes | pedem o mesmo recurso no mesmo horário ao mesmo tempo | operação normal, pico | serviço de reservas + banco | um pedido aceito; os outros recusados por conflito (409 na API) | 1 aceito, 19 recusados, 1 linha; 5 de 5 execuções | `DuplaReservaConcorrenciaIT`: 1 aceito em 20 para a sala em 5 repetições e para o professor em 1 execução; a recusa é conferida no serviço (`ConflitoDeHorarioException`), e a tradução em 409 está em `ApiDeReservasIT.rn02_conflitoDeHorarioResponde409ComCodigo`; sob carga, 1 aceita em 5.652 (`docs/performance/resultado-01.md`) | atende |
| CEN-02 | Solicitante autenticado | tenta alterar ou cancelar a reserva de outro pelo id | operação normal | API de reservas | acesso negado sem revelar a reserva alheia | 100% recusadas, 0 alterações | `MatrizDeAutorizacaoIT` RES-06/RES-07 (IDOR) → 403; 88 células verdes | atende |
| CEN-03 | Responsável | aprova uma reserva e a gravação falha no meio | falha de banco durante a transação | serviço de reservas + auditoria | nada é gravado: nem a mudança, nem a auditoria | 0 auditorias órfãs | `AuditoriaIT.rn09_03_rollbackNaoDeixaAuditoriaOrfa` | atende |
| CEN-04 | 50 usuários | consultam disponibilidade ao mesmo tempo | pico, máquina local | API de pesquisa | resposta dentro do limite | p95 ≤ 300 ms (proposta do grupo) | p95 **332 ms** na consulta e **506 ms** na criação disputada (`perf/resultados/rodada-01.jtl`) | **não atende** (DEF-003) |
| CEN-05 | Provedor de notificação | responde erro ou passa do timeout | operação normal | adaptador de notificação | reserva confirmada; falha registrada | reserva gravada em 100%; espera limitada a 2 s | `NotificadorHttpIT.int04_respostaLentaEhInterrompidaNoTimeout`, INT-02/03/05; `NotificacaoAposCommitIT` | atende |
| CEN-06 | Solicitante | pede reserva que termina exatamente no início da manutenção | operação normal | regra de manutenção | reserva aceita (intervalo semiaberto) | trinca −1 s, 0, +1 s | tabela de fronteiras em `aprovacao-bloqueio.feature`; `IntervaloTest` | atende |

## 3. Utility tree
| Atributo (ISO 25010) | Refinamento | Cenário | (Importância, Dificuldade) |
|---|---|---|---|
| Confiabilidade | Integridade sob concorrência | CEN-01 | (A, A) |
| Segurança | Autorização por perfil e posse | CEN-02 | (A, M) |
| Segurança | Responsabilização | CEN-03 | (A, M) |
| Eficiência de desempenho | Latência no pico | CEN-04 | (M, M) → revisado para **(M, A)**: a medição mostrou que a proteção do CEN-01 pesa na latência |
| Confiabilidade | Tolerância a falha externa | CEN-05 | (M, B) |
| Manutenibilidade | Testabilidade do tempo | CEN-06 | (M, B) |

## 4. Táticas com evidência no código
Caminhos relativos a `src/main/java/br/senac/organizacaorecursos/`, linhas no commit `16adf58`.

| Tática (Len Bass) | Onde está | Cenário |
|---|---|---|
| Transactions + restrição no banco | `src/main/resources/db/migration/V3__protecao_contra_dupla_reserva.sql` (exclusion constraint `reserva_sem_sobreposicao`, só estados que ocupam) | CEN-01 |
| Schedule Resources (serializar pelo recurso) | `persistencia/RecursoRepository.java:18-20` (`PESSIMISTIC_WRITE` em `buscarTravandoParaReserva`); usado em `servico/ServicoDeReservas.java:205` | CEN-01 |
| Tradução do conflito em 409 | `servico/ServicoDeReservas.java:225-233` (`DataIntegrityViolationException` da constraint → conflito) | CEN-01 |
| Authorize Actors (perfil) | `api/ReservaController.java:52`, `:68` (`@PreAuthorize`) e demais controllers | CEN-02 |
| Authorize Actors (posse) | `servico/ServicoDeReservas.java:199` (`pertenceA`) e `:116` (quem pediu não aprova) | CEN-02 |
| Audit + Timestamp + Transactions | `servico/ServicoDeReservas.java:191` (auditoria gravada na mesma transação, instante do `Clock`); `dominio/AuditoriaReserva.java:16` (`@Immutable`) | CEN-03 |
| Registro do conflito evitado mesmo com rollback | `servico/RegistroDeConflitos.java:27` (`REQUIRES_NEW`), chamado em `api/ReservaController.java:87` e `:100` depois da recusa | RF-12 |
| Timeout + Degradation | `notificacao/NotificadorHttp.java:27-28` (timeouts de conexão e leitura); `notificacao/ConfiguracaoDeNotificacao.java:45` (`AFTER_COMMIT`: notifica só depois de gravar) | CEN-05 |
| Limit Nondeterminism | `configuracao/ConfiguracaoDeTempo.java:12-13` (`Clock` como bean), injetado em `ServicoDeReservas`, `ServicoDeToken` e `RegistroDeConflitos`. Nos testes, `Clock.fixed` só em `ServicoDeReservasTest`; as trincas de limite passam instantes fixos direto às regras | CEN-06 |
| Intervalo semiaberto | `dominio/Intervalo.java:22-23` (`sobrepoe` com `isBefore` nos dois sentidos) | CEN-06 |

## 5. Descobertas
**Pontos de sensibilidade**
- `dominio/Intervalo.java:23`: trocar `isBefore` por "antes ou igual" faz reservas encostadas conflitarem. Confirmado na mutação M1 (`docs/testes/mutacao-manual.md`).
- O predicado `WHERE estado IN (...)` da constraint decide se reservas canceladas bloqueiam o recurso (R-01).

**Pontos de trade-off**
- **Integridade × latência (confirmado por medição):** a trava por recurso (adicionada pelo DEF-002) atende os pedidos do mesmo recurso um por vez. CEN-01 é atendido; em compensação, o p95 da criação disputada foi 506 ms (CEN-04, DEF-003). Na arquitetura proposta este trade-off aparecia como "integridade × throughput" na constraint; a medição mostrou que o custo maior está na fila da trava. Isso ainda é **hipótese** (`docs/performance/resultado-01.md`, próximo experimento).
- **Auditoria × privacidade:** a auditoria guarda quem fez cada mudança; os dados de demonstração usam o domínio `.invalid`.
- **Sem cache de disponibilidade:** decisão mantida. Um cache ajudaria CEN-04, mas poderia mostrar livre uma sala já reservada.

**Riscos**
- R-01 (constraint com canceladas): **tratado**. A constraint vale só para SOLICITADA, APROVADA e EM_USO, com teste em `ProtecaoNoBancoIT.reservaCanceladaNaoOcupaORecursoNoBanco`.
- R-02 (IDOR): **tratado** (CEN-02).
- R-03 (notificação dentro da transação): **tratado** (`AFTER_COMMIT`).
- R-04 (novo): desempenho abaixo da meta proposta sob disputa (DEF-003), **aberto**.
- R-05 (novo): CSRF desligado depende de a credencial nunca ir para cookie (ADR-007, proposta).
- R-06 (novo): token JWT não revogável antes de expirar, por até 60 min (ADR-003).
- R-07 (novo): Spring Boot 3.5 fora do suporte gratuito (ADR-001).

**Não-riscos**
- Queda do provedor de notificação não afeta reservas (CEN-05).
- Outra instância da aplicação não quebra a regra de sobreposição: a constraint está no banco, ponto único de verdade.

**Temas de risco**
- **Tempo:** as regras críticas dependem de instante e intervalo. Os serviços que leem "agora" recebem o `Clock`; as regras de intervalo recebem instantes prontos. Lacuna conhecida: `AuditoriaIT` confere o instante gravado contra o relógio do sistema (`Instant.now()`), por faixa.
- **Disputa pelo mesmo recurso:** integridade e desempenho são decididos no mesmo ponto (trava + constraint).

## 6. Divergências: arquitetura proposta × código
| # | Na arquitetura de 2026-10-31 | No código | Tratamento |
|---|---|---|---|
| 1 | Interface "pendente" (Q-01, D-02) | HTML + CSS + JS servidos pelo Spring (ADR-006) | arquitetura atualizada |
| 2 | Componentes `ServicoDeAprovacao`, `MaquinaDeEstados`, `ServicoDeAuditoria` | Aprovação, retirada e devolução em `ServicoDeReservas`; transições no enum `EstadoReserva`; auditoria gravada por `ServicoDeReservas` via `AuditoriaReservaRepository`. Existem ainda `ServicoDeRecursos`, `ServicoDeManutencao`, `ServicoDeRelatorios` e `RegistroDeConflitos` | diagrama de componentes atualizado |
| 3 | Tabela `MOVIMENTACAO_MATERIAL` | Não existe: retirada e devolução são transições (APROVADA → EM_USO → CONCLUIDA) registradas na auditoria | modelo de dados atualizado |
| 4 | "Conflitos evitados: detalhe na Fase 5" | Tabela `tentativa_recusada` (V4), gravada em transação própria | modelo de dados atualizado |
| 5 | "Quem decide é o banco" (só constraint) | Trava pessimista por recurso + constraint (DEF-002; ADR-002 já atualizado) | sequência 7.1 atualizada |
| 6 | Rotas sem prefixo (`/auth/login`, `/reservas`) | Prefixo `/api` | texto atualizado |
| 7 | Matriz requisito → evidência com itens "pendente" (Fases 4 e 5) | Todos implementados e testados | matriz preenchida |
| 8 | CEN-04 e CEN-05 "validação pendente" | Medidos (seção 2) | cenários atualizados |

## 7. Medidas de qualidade (Aula 07)
| Medida | Valor | Fonte |
|---|---|---|
| Cobertura de linhas / ramos (JaCoCo) | 98,0% / 95,0% | artifact `relatorio-de-cobertura` do run 41348450251; gate 80/70 no `verify` |
| Quality Gate do SonarCloud (`main`, `16adf58`) | **aprovado** | <https://sonarcloud.io/project/overview?id=guilhermevianacloud_organizacao-recursos> |
| Bugs · code smells · duplicação | 0 · 0 · 0,0% | SonarCloud, `main` |
| Vulnerabilidades | **1** (`java:S4502`, CSRF), justificada no ADR-007; nota de segurança D até ser marcada como revisada | SonarCloud, `main`; DEF-004 |

Estas medidas são do commit em que o ciclo foi feito (`16adf58`). As da versão avaliada estão em [`docs/evidencias/README.md`](../evidencias/README.md).

## 8. Ações priorizadas
| Prioridade | Ação | Responsável | Situação |
|---|---|---|---|
| 1 | Corrigir as divergências da seção 6 em `docs/arquitetura.md` | Claude | feito neste PR |
| 2 | Revisar o S4502 no SonarCloud (aceitar com a justificativa do ADR-007, ou reabrir a decisão) | Guilherme | pendente |
| 3 | DEF-003: aceitar o risco com nome e motivo, ou rodar o próximo experimento de carga | grupo | pendente |
| 4 | Perguntar ao professor se a restrição do cron diário de treino (Aula 04, Q-05) vale para este projeto | grupo | pendente |
| 5 | RF-14: a documentação OpenAPI só tem teste de fumaça, sem teste automatizado | — | aceito como baixo risco; sem ação nesta versão |
