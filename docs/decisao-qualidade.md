# Decisão de qualidade — v1.0.0-rc3

Aula 14 (Prompt 3). A decisão é proporcional à evidência: tudo foi medido em ambiente local e no CI, então esta
decisão **não** afirma prontidão para produção.

> **Situação: decidida em 2026-11-11.** Guilherme Viana Lopes e Gustavo Santana assumiram todos os riscos aceitos abaixo; a divisão do trabalho foi
> informada pelo grupo na mesma data (seção "O que cada integrante fez").

## Decisão
**Liberar com ressalvas** a versão `v1.0.0-rc3` para avaliação acadêmica, com os riscos abaixo assumidos por Guilherme Viana Lopes e Gustavo Santana (2026-11-11).

## Critérios e evidências
| Critério | Evidência | Atendido? |
|---|---|---|
| Aplicação executa seguindo só o README | clone limpo com repositório Maven vazio (`docs/entrega/checklist.md`); CI | sim |
| Pipeline de CI presente e bloqueando falha | `.github/workflows/ci.yml`; run vermelho intencional no README; branch protection na `main` | sim |
| Nenhuma falha de autorização | `MatrizDeAutorizacaoIT` — 88 células, inclusive IDOR | sim |
| Nenhuma dupla reserva | `DuplaReservaConcorrenciaIT` (20 simultâneos, 5 repetições); 1 aceita em 5.652 sob carga | sim |
| Cobertura ≥ 80% linhas / ≥ 70% branches | JaCoCo: 98,0% / 95,0% | sim |
| 100% dos requisitos críticos na RTM | `docs/rtm.md`: 9 de 9 cobertos (teste verde + mutação detectada) | sim |
| 0 bugs ou vulnerabilidades críticas | SonarCloud: Quality Gate aprovado; 11 de 12 apontamentos corrigidos, S4502 justificado (ADR-007) e aceito no SonarCloud em 2026-11-11 (status *Accepted*); nota de segurança A, 0 vulnerabilidades abertas; 0 defeitos críticos abertos no registro | sim |
| Desempenho p95 ≤ 300 ms (meta proposta) | rodada 01: 332 ms e 506 ms | não |

## Riscos aceitos
| Risco | Impacto | Justificativa | Responsável |
|---|---|---|---|
| DEF-003 — p95 acima da meta proposta | demora de até ~0,5 s em 5% das criações no pior caso | sem perda de dados nem erro; meta proposta pelo próprio grupo | Guilherme Viana Lopes e Gustavo Santana (2026-11-11) |
| CSRF desligado (S4502, ADR-007) | exposição a CSRF se a credencial passar a ir em cookie | API stateless com token no cabeçalho; condição de revisão no ADR-007 | Guilherme Viana Lopes e Gustavo Santana (2026-11-11) |
| Token JWT não revogável antes de expirar (ADR-003) | usuário desligado mantém acesso por até 60 min | validade curta; revogação imediata exigiria lista de bloqueio ou sessão (condição de revisão do ADR-003) | Guilherme Viana Lopes e Gustavo Santana (2026-11-11) |
| Spring Boot 3.5 fora do suporte gratuito (ADR-001) | vulnerabilidade nova sem correção pública | especificação exige 3.x | Guilherme Viana Lopes (decisão registrada no ADR-001, 2026-10-31); assumido também por Gustavo Santana (2026-11-11) |

## O que cada integrante fez
O código, os testes e a documentação do repositório foram escritos com um agente de IA (Claude), seguindo os prompts das
aulas práticas, como declarado no `README.md`. O grupo trabalhou junto usando uma única conta do GitHub (a de quem
tinha o plano do agente); por isso, em 2026-11-11, o histórico da `main` foi reescrito para registrar quem participou
de cada commit — autor principal e coautores (`Co-authored-by`) —, sem mudar nenhum arquivo. A divisão por assunto,
informada pelo grupo:

| Assunto | Commits | Participantes |
|---|---|---|
| Esqueleto do projeto e build dos testes de integração | 2 | Guilherme, Rafael, Gustavo |
| Pipeline de CI no GitHub Actions (quebra intencional e reversão, Aula 06) | 4 | Guilherme |
| Requisitos, arquitetura, plano de teste e RTM (Fase 2) | 1 | Guilherme, Gustavo |
| Regras de reserva com TDD: intervalo, sobreposição, estados e manutenção (RN-01, 02, 04, 07, 08) | 6 | Guilherme, Rafael, Gustavo |
| Concorrência: pedidos simultâneos e trava no banco (RN-03, DEF-001, DEF-002) | 3 | Gustavo |
| Login, autorização, API de reservas e auditoria (RF-01, RF-14, RN-09, RN-10, RN-12) | 5 | Guilherme |
| Pesquisa de recursos, relatório e notificação com WireMock (RF-03, RF-11, RF-12) | 4 | Rafael, Gustavo |
| Cenários BDD com Cucumber | 1 | Guilherme, Rafael |
| Interface web e testes ponta a ponta com Playwright (RF-13, DEF-005, DEF-006) | 5 | Guilherme, Rafael, Gustavo |
| Cobertura (JaCoCo), SonarCloud e correções da análise | 4 | Guilherme, Gustavo |
| Desempenho com JMeter (DEF-003) e mutação manual (Aula 12) | 4 | Guilherme, Gustavo |
| Documentação e entrega: RTM, relatório, prova oral, checklist, evidências, ATAM, revisão | 11 | Guilherme, Rafael, Gustavo |
| Identidade visual da interface (CSS) | 4 | Guilherme |

| Integrante | O que fez |
|---|---|
| Guilherme Viana Lopes | Conduziu o agente na conta do grupo; CI, autenticação/autorização/API/auditoria e identidade visual; participou das regras de reserva, requisitos e arquitetura, BDD, interface, cobertura/SonarCloud, desempenho/mutação e documentação |
| Rafael Souza Silva | Pesquisa de recursos, relatório e notificação (com Gustavo); participou do esqueleto, das regras de reserva, do BDD, da interface e testes ponta a ponta e da documentação |
| Gustavo Santana | Concorrência e trava no banco (RN-03); pesquisa, relatório e notificação (com Rafael); participou do esqueleto, de requisitos e arquitetura, das regras de reserva, da interface, de cobertura/SonarCloud, de desempenho/mutação e da documentação |

## Texto para o formulário de entrega
Turma STADSCAS4NB (quinta). Integrantes: Guilherme Viana Lopes, Rafael Souza Silva, Gustavo Santana.
Repositório: https://github.com/guilhermevianacloud/organizacao-recursos — versão avaliada: tag `v1.0.0-rc3`.
