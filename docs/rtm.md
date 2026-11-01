# Matriz de Rastreabilidade (RTM)

Liga **requisito → risco → teste → evidência** (especificação do projeto). Cada linha abre um arquivo que existe e
aponta uma execução real do CI. "Testado" sem execução não conta.
Meta: **100% dos requisitos críticos com evidência** do CI no commit avaliado.

Caminhos de teste relativos a `src/test/java/br/senac/organizacaorecursos/` (features em `src/test/resources/features/`).
Evidência atual: [CI run 41322975920](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41322975920)
(Fase 5, unidade 85 + integração 167, 0 falhas). Situação em 2026-11-01.

**Legenda de status:** pendente · verde (teste passando no CI) · coberto (verde + mutação manual confirmou que o teste detecta a quebra — Fase 7).

## Requisitos críticos
| Requisito / regra | Risco | Teste (caminho e nome) | Nível | Evidência | Status |
|---|---|---|---|---|---|
| RF-06 / RN-03 — dupla reserva | RP-01 · DEF-001, DEF-002 | `servico/DuplaReservaConcorrenciaIT`; `servico/ProtecaoNoBancoIT`; `api/ApiDeReservasIT.rn02_conflitoDeHorarioResponde409ComCodigo` | concorrência + integração + API | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RF-05 / RN-02 — sobreposição (sala, material, professor) | RP-03 | `dominio/IntervaloTest` (`Sobreposicao`); `servico/ServicoDeReservasIT` (`Sobreposicao`) | unidade + integração | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RN-01 — término após início | RP-03 | `dominio/IntervaloTest` (`TerminoPosteriorAoInicio`); `api/ApiDeReservasIT.rn01_terminoAntesDoInicioResponde400Compreensivel`; `servico/ProtecaoNoBancoIT.bancoRecusaTerminoAntesDoInicio` | unidade + API + banco | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RF-08 / RN-04 — manutenção | RP-03 | `servico/ServicoDeReservasIT` (`EmManutencao`); `aprovacao-bloqueio.feature` (esquema de manutenção) | integração + BDD | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RF-07 / RN-05, RN-06 — aprovação de restrito | RP-02 | `aprovacao-bloqueio.feature` (3 cenários); `api/MatrizDeAutorizacaoIT` (RES-08, RES-09); `dominio/ReservaTest` | BDD + API + unidade | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RN-07 — máquina de estados | — | `dominio/EstadoReservaTest` (49 pares); `dominio/ReservaTest`; `aprovacao-bloqueio.feature` (esquema de transições) | unidade + BDD | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RN-08 — reserva iniciada não é apagada | — | `dominio/ReservaTest.rn08_reservaIniciadaNaoPodeSerApagada`; API sem rota de exclusão de reserva; cancelar mantém a linha (`servico/AuditoriaIT.cancelamentoRegistraTransicaoEMotivo`) | unidade + integração | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RF-10 / RN-09 — auditoria | RP-04 | `servico/AuditoriaIT` (6 testes, inclusive rollback); `api/MatrizDeAutorizacaoIT` (RES-13) | integração + API | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |
| RF-01 / RN-10 — autorização e posse (IDOR) | RP-02 | `api/MatrizDeAutorizacaoIT` (22 operações × 4 perfis = 88 células, lidas de `docs/seguranca/matriz-autorizacao.md`); `api/ApiDeReservasIT` (login, token adulterado) | API parametrizada | run 41322975920 · mutação: `docs/testes/mutacao-manual.md` | coberto |

## Demais requisitos
| Requisito / regra | Risco | Teste | Nível | Evidência | Status |
|---|---|---|---|---|---|
| RF-02 — cadastros | — | `api/MatrizDeAutorizacaoIT` (REC-03, REC-04); `api/ApiDeReservasIT.rn12_01_capacidadeNegativaResponde400ApontandoOCampo` | API | run 41322975920 | verde |
| RF-03 — pesquisa | — | `api/PesquisaDeRecursosIT` (11 testes) | API | run 41322975920 | verde |
| RF-04 — criar, alterar, cancelar | RP-02 | `api/MatrizDeAutorizacaoIT` (RES-03..07); `api/ApiDeReservasIT` | API | run 41322975920 | verde |
| RF-09 — retirada e devolução | — | `api/MatrizDeAutorizacaoIT` (RES-10..12); `aprovacao-bloqueio.feature` | API + BDD | run 41322975920 | verde |
| RF-11 / RN-13 — notificação | RP-06 | `notificacao/NotificadorHttpIT` (INT-01..06); `notificacao/NotificacaoAposCommitIT` | WireMock + integração | run 41322975920 | verde |
| RF-12 / RN-11 — relatório de utilização | — | `api/RelatorioDeUtilizacaoIT` (valores calculados à mão) | API caixa-preta | run 41322975920 | verde |
| RF-13 / RN-12 — interface e mensagens | RP-05 | `api/ApiDeReservasIT` (400 compreensível, sem detalhe interno); `e2e/InterfaceE2EIT` (Playwright: fluxo Solicitante → Responsável, login inválido, sem login) | API + end-to-end | [run 41324432479](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41324432479) | verde |
| RF-14 — documentação da API | — | OpenAPI em `/swagger-ui.html` (teste de fumaça); README | revisão | — | verde (sem teste automatizado) |

## Não funcionais
| RNF | Medida | Teste / instrumento | Evidência | Status |
|---|---|---|---|---|
| RNF-01, RNF-02 — desempenho | p95 ≤ 300 ms, 50 usuários | JMeter: `perf/reservas.jmx`, `perf/resultados/rodada-01.jtl`, `docs/performance/resultado-01.md` | rodada 01 (2026-11-01) | **não atende** — p95 332 ms (consulta) e 506 ms (criação); DEF-003 aberto |
| RNF-03 — integridade | 1 aceito em 20, 5 de 5 | `servico/DuplaReservaConcorrenciaIT` | run 41322975920 | verde |
| RNF-04 — autorização | 100% da matriz | `api/MatrizDeAutorizacaoIT` | run 41322975920 | verde |
| RNF-05 — senha e token | BCrypt; 60 min; 0 segredos | `api/ApiDeReservasIT` (login); SonarCloud | run 41322975920; [run 41346988995](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41346988995) | verde: testes; SonarCloud sem segredo apontado (S4502 justificado no ADR-007) |
| RNF-06 — erro neutro | 0 vazamentos | `api/ApiDeReservasIT` (`semDetalheInterno`) | run 41322975920 | verde |
| RNF-07 — falha externa | timeout 2 s; reserva mantida | `notificacao/NotificadorHttpIT.int04_…`; `notificacao/NotificacaoAposCommitIT` | run 41322975920 | verde |
| RNF-08 — auditoria | 0 órfãos | `servico/AuditoriaIT.rn09_03_rollbackNaoDeixaAuditoriaOrfa` | run 41322975920 | verde |
| RNF-09 — interface | 360–1920 px | `e2e/InterfaceE2EIT.telasCabemEmLarguraDeCelularSemRolagemLateral` (360 px) | [run 41324432479](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41324432479) | verde |
| RNF-10 — cobertura e análise | 80/70; 0 críticos | JaCoCo no `verify` (linhas 98,0%, branches 95,0%; `docs/testes/lacunas-de-cobertura.md`); SonarCloud | [run 41346988995](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41346988995) | parcial: JaCoCo verde; Quality Gate do SonarCloud aprovado (PR #10); 1 apontamento (S4502) justificado e aguardando revisão no site (DEF-004) |
| RNF-12 — instalabilidade | 1 comando | `OrganizacaoRecursosApplicationIT` + CI + teste de fumaça | run 41322975920 | verde |
| RNF-13 — testabilidade | 5 execuções sem intermitência | `servico/DuplaReservaConcorrenciaIT` (`@RepeatedTest(5)`) | run 41322975920 | verde |
| RNF-14 — privacidade | 0 dados reais; 0 tokens em log | dados `.invalid`; `notificacao/NotificadorHttpIT.int06_…` | run 41322975920 | verde |

## Percentual de requisitos críticos com evidência
**9 de 9 (100%) cobertos:** teste verde no CI e mutação manual detectada para cada regra crítica (`docs/testes/mutacao-manual.md`, 10 mutações, 10 detectadas).
