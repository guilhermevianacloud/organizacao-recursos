# Status dos requisitos

Aula 11. Os 14 requisitos funcionais obrigatórios e as regras críticas da especificação comparados com o código,
os testes e a RTM. **Só está "feito" o que tem teste que falharia se a regra quebrasse.**
Evidência: [CI run 41475669466](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41475669466) (98 + 187 testes, 0 falhas). Situação em 2026-11-02.

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
| RF-01 Autenticação e autorização por perfil | feito | `api/MatrizDeAutorizacaoIT`, `api/ApiDeReservasIT` (login, token adulterado, token vencido) | limitador de nota |
| RF-02 Cadastro e consulta de recursos | feito | `api/MatrizDeAutorizacaoIT` (REC-01..04), `api/ApiDeReservasIT.rn12_01_…` | sem dados para reservar |
| RF-03 Pesquisa (tipo, capacidade, localização, competência, disponibilidade) | feito | `api/PesquisaDeRecursosIT` | usuário não acha recurso livre |
| RF-04 Criar, alterar e cancelar reservas | feito | `api/MatrizDeAutorizacaoIT` (RES-03..07), `servico/ServicoDeReservasIT` | função central ausente |
| RF-05 Sobreposição, inclusive agenda do professor | feito | `dominio/IntervaloTest`, `servico/ServicoDeReservasIT.rn02_10_…` | dupla alocação |
| RF-06 Proteção contra dupla reserva simultânea | feito | `servico/DuplaReservaConcorrenciaIT` | limitador de nota |
| RF-07 Aprovação obrigatória de restritos | feito | `aprovacao-bloqueio.feature`, `api/MatrizDeAutorizacaoIT` (RES-08), `servico/ServicoDeReservasTest.rn06_…` | uso indevido de recurso restrito |
| RF-08 Bloqueio em manutenção | feito | `servico/ServicoDeReservasIT` (`EmManutencao`), feature | reserva em sala interditada |
| RF-09 Retirada e devolução | feito | `api/MatrizDeAutorizacaoIT` (RES-10, RES-11), feature (transições) | material sem controle |
| RF-10 Histórico auditável | feito | `servico/AuditoriaIT` | sem responsabilização |
| RF-11 Notificação | feito | `notificacao/NotificadorHttpIT`, `notificacao/NotificacaoAposCommitIT` | requisito obrigatório ausente |
| RF-12 Relatório de utilização | feito | `api/RelatorioDeUtilizacaoIT` | requisito obrigatório ausente |
| RF-13 Interface responsiva e mensagens compreensíveis | feito | mensagens: `api/ApiDeReservasIT`; interface: `e2e/InterfaceE2EIT` (fluxos, 360 px, campos vazios dos DEF-005/006) | avaliação de usabilidade |
| RF-14 Documentação da API ou dos fluxos | feito | `api/DocumentacaoDaApiIT` (OpenAPI e Swagger sem login); README | avaliador não entende o uso |

## Regras críticas
| Regra | Situação | Evidência | Risco se ficar de fora |
|---|---|---|---|
| Término posterior ao início (RN-01) | feito | `IntervaloTest`, `ApiDeReservasIT.rn01_…` | reserva sem duração quebra relatório e agenda |
| Sobreposição de sala, material e professor (RN-02) | feito | `IntervaloTest`, `ServicoDeReservasIT` | dupla alocação |
| Dupla reserva (RN-03) | feito | `DuplaReservaConcorrenciaIT` | limitador de nota |
| Manutenção (RN-04) | feito | `ServicoDeReservasIT`, feature | reserva em sala interditada |
| Aprovação de restrito (RN-05, RN-06) | feito | feature, `MatrizDeAutorizacaoIT`, `ServicoDeReservasTest.rn06_…` | uso indevido; quem pede aprova a si mesmo |
| Máquina de estados (RN-07) | feito | `EstadoReservaTest`, feature (inclusive REJEITADA e NAO_COMPARECEU) | histórico incoerente |
| Reserva iniciada não apagada (RN-08) | feito | `ReservaTest`, `AuditoriaIT` | perda de rastro de uso |
| Auditoria (RN-09) | feito | `AuditoriaIT` | sem responsabilização |
| Autorização (RN-10) | feito | `MatrizDeAutorizacaoIT` | limitador de nota |

## Fases até a entrega (semana 46)
Executadas por Guilherme Viana Lopes e Claude (agente de IA), em ordem:
1. Fase 6 — feito: JaCoCo 80/70 (98,0% / 95,0%) e end-to-end ([run 41324432479](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41324432479)). SonarCloud configurado em 2026-11-01; os apontamentos da 1ª análise estão no DEF-004.
2. Fase 7 — feito: JMeter (`docs/performance/resultado-01.md`, DEF-003) e mutação manual (10 de 10; a M11 entrou em 2026-11-02).
3. Fase 8 — feito: `docs/relatorio-qualidade.md` e `docs/prova-oral/`.
4. Fase 9 — feito: clone limpo (`docs/entrega/checklist.md`) e pacote de evidências (`docs/evidencias/README.md`).

## Pendências até a entrega (12/11), com responsável
A Aula 11 pede a divisão das pendências entre os integrantes. As que dependem do grupo ainda **não têm responsável definido**: a divisão é uma decisão do grupo e não foi feita em nome de ninguém.

| Pendência | Por que importa | Responsável |
|---|---|---|
| Revisar o apontamento S4502 no SonarCloud (aceitar com a justificativa do ADR-007 ou reabrir) | meta "0 vulnerabilidades críticas" | Guilherme Viana Lopes (dono da conta no SonarCloud) |
| Decidir o aceite dos riscos (DEF-003, CSRF, token não revogável) e quem os assume | decisão de qualidade da Aula 14 | *a definir pelo grupo* |
| Declaração de cada integrante em `docs/decisao-qualidade.md` | autoria (10% da nota) | cada integrante, a própria |
| Revisão por pares nos pull requests | entregável "repositório com revisão por pares" | Rafael Souza e Gustavo Santana (a confirmar) |
| Ensaio da prova oral com notas em `docs/prova-oral/ensaio.md` | preparação da S47 | os três |
| Envio no formulário com a tag avaliada | entrega | *a definir pelo grupo* (um integrante) |

