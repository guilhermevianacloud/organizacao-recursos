# Decisão de qualidade — v1.0.0-rc2

Aula 14 (Prompt 3). A decisão é proporcional à evidência: tudo foi medido em ambiente local e no CI, então esta
decisão **não** afirma prontidão para produção.

> **Situação: rascunho.** Os campos marcados *a confirmar* dependem do grupo (quem aceita cada risco e a declaração de
> cada integrante). Nada foi preenchido em nome de outra pessoa.

## Decisão
**Liberar com ressalvas** a versão `v1.0.0-rc2` para avaliação acadêmica. *(a confirmar pelo grupo)*

## Critérios e evidências
| Critério | Evidência | Atendido? |
|---|---|---|
| Aplicação executa seguindo só o README | clone limpo com repositório Maven vazio (`docs/entrega/checklist.md`); CI | sim |
| Pipeline de CI presente e bloqueando falha | `.github/workflows/ci.yml`; run vermelho intencional no README; branch protection na `main` | sim |
| Nenhuma falha de autorização | `MatrizDeAutorizacaoIT` — 88 células, inclusive IDOR | sim |
| Nenhuma dupla reserva | `DuplaReservaConcorrenciaIT` (20 simultâneos, 5 repetições); 1 aceita em 5.652 sob carga | sim |
| Cobertura ≥ 80% linhas / ≥ 70% branches | JaCoCo: 98,0% / 95,0% | sim |
| 100% dos requisitos críticos na RTM | `docs/rtm.md`: 9 de 9 cobertos (teste verde + mutação detectada) | sim |
| 0 bugs ou vulnerabilidades críticas | SonarCloud: Quality Gate aprovado; 11 de 12 apontamentos corrigidos, S4502 justificado (ADR-007, proposta); 0 defeitos críticos abertos no registro | parcial, até a revisão do S4502 |
| Desempenho p95 ≤ 300 ms (meta proposta) | rodada 01: 332 ms e 506 ms | não |

## Riscos aceitos
| Risco | Impacto | Justificativa | Responsável |
|---|---|---|---|
| DEF-003 — p95 acima da meta proposta | demora de até ~0,5 s em 5% das criações no pior caso | sem perda de dados nem erro; meta proposta pelo próprio grupo | *a confirmar* |
| CSRF desligado (S4502, ADR-007) | exposição a CSRF se a credencial passar a ir em cookie | API stateless com token no cabeçalho; condição de revisão no ADR-007 | *a confirmar* |
| Spring Boot 3.5 fora do suporte gratuito (ADR-001) | vulnerabilidade nova sem correção pública | especificação exige 3.x | Guilherme Viana Lopes (decisão registrada no ADR-001, 2026-10-31) |

## O que cada integrante fez
O código, os testes e a documentação do repositório foram escritos com um agente de IA (Claude), seguindo os prompts das
aulas práticas, como declarado no `README.md`. Cada integrante declara a própria participação:

| Integrante | O que fez |
|---|---|
| Guilherme Viana Lopes | *a preencher pelo próprio integrante* |
| Rafael Souza | *a preencher pelo próprio integrante* |
| Gustavo Santana | *a preencher pelo próprio integrante* |

## Texto para o formulário de entrega
Turma STADSCAS4NB (quinta). Integrantes: Guilherme Viana Lopes, Rafael Souza, Gustavo Santana.
Repositório: https://github.com/guilhermevianacloud/organizacao-recursos — versão avaliada: tag `v1.0.0-rc2`.
