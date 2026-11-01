# Organização de Recursos — Plano de Teste

Estrutura conforme a ISO/IEC/IEEE 29119-3 (Aula 02). Requisitos e regras em [`docs/prd.md`](prd.md); rastreabilidade em [`docs/rtm.md`](rtm.md); inventário de casos em [`docs/testes/plano-tdd.md`](testes/plano-tdd.md).
Versão 1 — 2026-10-31. Revisar ao fim de cada fase.

## 1. Estratégia
- **Teste guia o código (TDD):** para cada regra de negócio, primeiro o teste falhando pela razão esperada, depois o código mínimo, depois a refatoração. O histórico mostra `test(red)` → `feat(green)` → `refactor`.
- **Regras críticas primeiro:** os limitadores de nota (dupla reserva, autorização, pipeline, aplicação que executa) têm prioridade sobre o restante.
- **Ambiente real para o que é crítico:** persistência e integrações são verificadas com PostgreSQL real (Testcontainers) e WireMock; mock (Mockito) só em teste de unidade isolado. **[Especificação]**
- **Oráculo verificável:** todo teste tem asserção sobre o resultado; "não lançou exceção" sozinho não é aceito.
- **Cobertura é diagnóstico, não meta isolada:** lacunas são classificadas em risco ou cosmética; só as de risco recebem teste.
- **Prova de que o teste protege:** mutação manual nas regras críticas (Aula 12).

## 2. Escopo
### 2.1 Itens a testar
Todos os requisitos RF-01 a RF-14, as regras RN-01 a RN-13 e os não funcionais RNF-01 a RNF-14 do PRD.

### 2.2 Itens que não serão testados (e por quê)
| Item | Motivo |
|---|---|
| Provedor de notificação real | Não existe; RF-11 permite simulação. Testamos o contrato com WireMock. |
| Desempenho em ambiente de produção | Não há produção; a carga roda em máquina local e a conclusão registra esse limite. |
| Compatibilidade com navegadores antigos | Fora do escopo do produto; a interface é testada em navegador atual. |
| Bibliotecas de terceiros (Spring, PostgreSQL) | Confiadas; testamos o nosso uso delas, não o funcionamento interno. |

## 3. Níveis e tipos de teste
| Nível / tipo | O que cobre | Ferramenta | Onde | Responsável |
|---|---|---|---|---|
| Unidade | Regras de intervalo, manutenção, máquina de estados, validações | JUnit 5, AssertJ, Mockito (isolado) | `src/test/java` | Claude (escrita) · Guilherme (revisão) |
| Unidade parametrizada | Fronteiras (−1, 0, +1), tabela de transições, matriz de autorização | `@ParameterizedTest` + `@CsvSource` / `@MethodSource` | `src/test/java` | idem |
| Integração | JPA, migrações, constraint de sobreposição, auditoria na mesma transação, rollback | Testcontainers (PostgreSQL 17) | `src/test/java` | idem |
| Concorrência | Dupla reserva em sala e na agenda do professor | Testcontainers + `ExecutorService` + `CountDownLatch` | `src/test/java` | idem |
| API caixa-preta | Contrato HTTP: status, corpo, ProblemDetail, relatório com valor calculado à mão | MockMvc | `src/test/java` | idem |
| BDD | Aprovação de restrito, manutenção, transições | Cucumber-JVM | `src/test/resources/features` | idem |
| Integração externa | Notificação: matriz INT-01..INT-06 | WireMock | `src/test/java` | idem |
| Segurança | Matriz perfil × operação, posse (IDOR), erro neutro | MockMvc parametrizado | `src/test/java` | idem |
| End-to-end | Fluxo login → reservar → aprovar pela interface | a definir com a interface (Q-01) | — | idem |
| Carga | Disponibilidade e criação sob disputa | Apache JMeter 5.6 (modo CLI) | `perf/` | idem |
| Mutação manual | Cada regra crítica quebrada derruba um teste | git | `docs/testes/mutacao-manual.md` | idem |

## 4. Técnicas de projeto de casos
- **Partição de equivalência** — sempre com a classe inválida (ex.: capacidade negativa, término antes do início).
- **Análise de valor limite** — trinca −1 / 0 / +1 nas fronteiras de intervalo, manutenção e capacidade.
- **Tabela de decisão** — transições de estado e matriz de autorização, executadas como teste parametrizado.
- **Caixa-cinza** — conhece a regra, testa pela API.
- **Formato de caso de teste** — ID, título, pré-condições, dados de entrada concretos, passos numerados, resultado esperado mensurável, rastreabilidade (Aula 02).

## 5. Critérios
| Critério | Definição |
|---|---|
| **Entrada** (para começar a testar uma mudança) | Requisito e regra com ID no PRD; caso listado no plano TDD; build compilando |
| **Saída** (para considerar pronto) | `./mvnw -B verify` verde local e no CI; cobertura ≥ 80% linhas e ≥ 70% branches; Quality Gate do SonarCloud aprovado; linha da RTM com evidência do CI |
| **Suspensão** (parar e corrigir antes de seguir) | CI vermelho na `main`; teste intermitente detectado; defeito de severidade crítica aberto |
| **Retomada** | Defeito registrado em `docs/defeitos/` com teste de regressão e CI verde |

## 6. Metas
| Meta | Valor | Origem |
|---|---|---|
| Cobertura de linhas | ≥ 80% | Especificação |
| Cobertura de branches | ≥ 70% | Especificação |
| Requisitos críticos na RTM com evidência | 100% | Especificação |
| Bugs ou vulnerabilidades críticas | 0 | Especificação |
| Execuções seguidas do teste de concorrência sem falha | 5 de 5 | Aula 10 |

## 7. Riscos
### 7.1 De produto
| ID | Risco | Mitigação por teste |
|---|---|---|
| RP-01 | Dupla reserva sob concorrência | Teste de concorrência + constraint (ADR-002) |
| RP-02 | Solicitante altera reserva alheia (IDOR) | Casos de posse na matriz de autorização |
| RP-03 | Erro de fronteira na sobreposição (`<` × `<=`) | Trincas de limite + mutação manual |
| RP-04 | Auditoria órfã após falha | Integração com rollback forçado |
| RP-05 | Detalhe interno vazando em resposta de erro | Testes sobre o corpo do ProblemDetail |
| RP-06 | Falha do provedor de notificação derrubando reserva | WireMock com 500 e timeout |

### 7.2 De projeto
| ID | Risco | Mitigação |
|---|---|---|
| RJ-01 | Prazo curto (entrega em 12/11/2026) | Limitadores primeiro; fases com PR pequeno |
| RJ-02 | Código gerado por IA sem domínio do grupo | Revisão de cada PR; guia de estudo; ensaio da prova oral |
| RJ-03 | Teste intermitente por tempo ou concorrência | `Clock` injetado; nunca `Thread.sleep`; repetir 5 vezes |
| RJ-04 | Spring Boot 3.5 fora do suporte gratuito | ADR-001; acompanhar alertas do SonarCloud |

## 8. Ambiente
- **Local:** Windows 11, JDK 21 Temurin, Docker Desktop (PostgreSQL 17 via Testcontainers e `compose.yaml`).
- **CI:** GitHub Actions, `ubuntu-latest`, Java 21 Temurin, Docker do próprio runner.
- **Carga:** máquina local, aplicação local; nunca ambiente compartilhado.

## 9. Dados de teste
- Somente **dados sintéticos**, com e-mails no domínio reservado `.invalid` (ex.: `marina@exemplo.invalid`).
- Usuários de teste dos três perfis criados pela massa de teste, nunca com senha real.
- Cada teste de integração começa com banco limpo ou com dados que ele mesmo cria.

## 10. Cronograma
| Quando | O que entra em teste |
|---|---|
| Fase 3 (02–03/11) | Regras de intervalo, estados, constraint e concorrência |
| Fase 4 (03/11) | Autorização e validação |
| Fase 5 (04/11) | Auditoria, relatório, materiais, notificação, BDD |
| Fase 6 (05–06/11) | Cobertura, SonarCloud, end-to-end |
| Fase 7 (07–08/11) | Carga, mutação manual, RTM completa |
| Fase 8 (09–10/11) | Relatório de testes no relatório de qualidade |
