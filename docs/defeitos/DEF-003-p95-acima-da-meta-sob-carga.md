# DEF-003 — p95 acima da meta proposta sob carga

| Campo | Valor |
|---|---|
| Identificador | DEF-003 |
| Situação | **aberto** — analisado, correção ainda não feita; aceite do risco aguardando decisão do grupo |
| Requisito afetado | RNF-01 e RNF-02 (metas propostas pelo grupo em `docs/performance/rnf.md`) |
| Encontrado por | Rodada 01 do JMeter (`docs/performance/resultado-01.md`) |
| Data | 2026-11-01 |

## Resumo observável
Com 50 usuários simultâneos, o p95 da consulta de disponibilidade foi 332 ms e o da criação de reserva disputando o
mesmo recurso foi 506 ms; a meta proposta é 300 ms. Integridade e "nunca 500" foram atendidos.

## Ambiente / versão
Commit `644787f`; aplicação local com PostgreSQL em container; JMeter 5.6.3 na mesma máquina (i7-11390H, 15,7 GB).

## Passos para reproduzir
1. `./mvnw spring-boot:run` (banco recém-criado).
2. `jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01`.
3. Calcular p95 por operação a partir do `.jtl`.

## Resultado obtido × esperado
Obtido: p95 332 ms (consulta) e 506 ms (criação). Esperado: p95 ≤ 300 ms.

## Evidência
`perf/resultados/rodada-01.jtl` (18.899 amostras) e a tabela em `docs/performance/resultado-01.md`.

## Frequência, alcance e impacto
Sob o cenário de pior caso (todos no mesmo recurso, sem pausa). Usuário percebe demora de até ~0,5 s em 5% das
criações; nenhum dado incorreto.

## Severidade (com justificativa)
**Média** — degrada a experiência no pico, sem perda ou corrupção de dados e sem erro para o usuário.

## Prioridade (com justificativa)
**P2** — a meta é uma proposta do grupo (não da especificação); integridade, autorização e regras críticas vêm antes.
Corrigir depois da entrega, ou antes se sobrar tempo.

## Causa raiz (5 porquês) — hipótese, ainda não confirmada por medição
1. Por que o p95 passou de 300 ms? Porque os pedidos esperaram antes de serem atendidos.
2. Por que esperaram? Porque as criações disputam a mesma linha travada e todos os pedidos dividem 10 conexões.
3. Por que dividem só 10 conexões? Porque o pool ficou no tamanho padrão, sem dimensionamento.
4. Por que não foi dimensionado? Porque a meta de desempenho só foi medida agora, na Fase 7.
5. Condição controlável: **capacidade (pool e fila da trava) não foi dimensionada contra o modelo de carga antes da medição.**

## Correção proposta
Ver "Próximo experimento" em `docs/performance/resultado-01.md`: medir a espera do pool, repetir com o gerador em
outra máquina, depois ajustar uma variável por vez.

## Risco aceito?
**Proposta, aguardando decisão do grupo:** aceitar o risco na entrega `v1.0.0-rc1`, com a justificativa de que o
impacto é só no tempo de resposta no pior caso, a meta foi proposta pelo próprio grupo e as regras críticas estão
intactas. Quem aceitar (nome e motivo) fica registrado em `docs/decisao-qualidade.md`; até lá, o defeito segue aberto.
