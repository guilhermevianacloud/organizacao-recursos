# Status dos requisitos

Aula 11. Os 14 requisitos funcionais obrigatórios e as regras críticas da especificação comparados com o código,
os testes e a RTM. **Só está "feito" o que tem teste que falharia se a regra quebrasse.**
Evidência: [CI run 41322975920](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41322975920). Situação em 2026-11-01.

## Limitadores de nota (primeiro)
| Limitador | Situação | Evidência |
|---|---|---|
| Aplicação que não executa | feito | `OrganizacaoRecursosApplicationIT`; teste de fumaça com `./mvnw spring-boot:run` (ADR-006); clone limpo na Fase 9 |
| Pipeline ausente | feito | `.github/workflows/ci.yml`; runs no README; branch protection na `main` |
| Falha de autorização | feito | `MatrizDeAutorizacaoIT` (88 células, inclusive IDOR) |
| Possibilidade de dupla reserva | feito | `DuplaReservaConcorrenciaIT` + `ProtecaoNoBancoIT` (DEF-001, DEF-002) |

## Requisitos funcionais
| Requisito | Situação | Evidência (arquivo e teste) | Risco se ficar de fora |
|---|---|---|---|
| RF-01 Autenticação e autorização por perfil | feito | `api/MatrizDeAutorizacaoIT`, `api/ApiDeReservasIT` (login, token adulterado) | limitador de nota |
| RF-02 Cadastro e consulta de recursos | feito | `api/MatrizDeAutorizacaoIT` (REC-01..04), `api/ApiDeReservasIT.rn12_01_…` | sem dados para reservar |
| RF-03 Pesquisa (tipo, capacidade, localização, competência, disponibilidade) | feito | `api/PesquisaDeRecursosIT` | usuário não acha recurso livre |
| RF-04 Criar, alterar e cancelar reservas | feito | `api/MatrizDeAutorizacaoIT` (RES-03..07), `servico/ServicoDeReservasIT` | função central ausente |
| RF-05 Sobreposição, inclusive agenda do professor | feito | `dominio/IntervaloTest`, `servico/ServicoDeReservasIT.rn02_10_…` | dupla alocação |
| RF-06 Proteção contra dupla reserva simultânea | feito | `servico/DuplaReservaConcorrenciaIT` | limitador de nota |
| RF-07 Aprovação obrigatória de restritos | feito | `aprovacao-bloqueio.feature`, `api/MatrizDeAutorizacaoIT` (RES-08) | uso indevido de recurso restrito |
| RF-08 Bloqueio em manutenção | feito | `servico/ServicoDeReservasIT` (`EmManutencao`), feature | reserva em sala interditada |
| RF-09 Retirada e devolução | feito | `api/MatrizDeAutorizacaoIT` (RES-10, RES-11), feature (transições) | material sem controle |
| RF-10 Histórico auditável | feito | `servico/AuditoriaIT` | sem responsabilização |
| RF-11 Notificação | feito | `notificacao/NotificadorHttpIT`, `notificacao/NotificacaoAposCommitIT` | requisito obrigatório ausente |
| RF-12 Relatório de utilização | feito | `api/RelatorioDeUtilizacaoIT` | requisito obrigatório ausente |
| RF-13 Interface responsiva e mensagens compreensíveis | parcial | mensagens: `api/ApiDeReservasIT`; interface: teste de fumaça — falta o end-to-end (Fase 6) | avaliação de usabilidade |
| RF-14 Documentação da API ou dos fluxos | feito (revisão) | OpenAPI em `/swagger-ui.html`; README | avaliador não entende o uso |

## Regras críticas
| Regra | Situação | Evidência |
|---|---|---|
| Término posterior ao início (RN-01) | feito | `IntervaloTest`, `ApiDeReservasIT.rn01_…` |
| Sobreposição de sala, material e professor (RN-02) | feito | `IntervaloTest`, `ServicoDeReservasIT` |
| Dupla reserva (RN-03) | feito | `DuplaReservaConcorrenciaIT` |
| Manutenção (RN-04) | feito | `ServicoDeReservasIT`, feature |
| Aprovação de restrito (RN-05, RN-06) | feito | feature, `MatrizDeAutorizacaoIT` |
| Máquina de estados (RN-07) | feito | `EstadoReservaTest`, feature |
| Reserva iniciada não apagada (RN-08) | feito | `ReservaTest`, `AuditoriaIT` |
| Auditoria (RN-09) | feito | `AuditoriaIT` |
| Autorização (RN-10) | feito | `MatrizDeAutorizacaoIT` |

## Pendências até a entrega (semana 46)
Executadas por Guilherme Viana Lopes e Claude (agente de IA), em ordem:
1. Fase 6 — JaCoCo 80/70 no `verify`, SonarCloud no PR (precisa da conta e do `SONAR_TOKEN` do Guilherme), teste end-to-end da interface (fecha RF-13).
2. Fase 7 — JMeter (RNF-01, RNF-02) e mutação manual das regras críticas.
3. Fase 8 — relatório de qualidade e preparação da prova oral.
4. Fase 9 — clone limpo, tag `v1.0.0-rc1`, pacote de evidências e envio.
