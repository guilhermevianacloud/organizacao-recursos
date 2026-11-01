# Resultado da rodada 01 de carga

Aula 10. Requisito escrito antes da rodada: [`rnf.md`](rnf.md) (commit `644787f`). Plano: [`perf/reservas.jmx`](../../perf/reservas.jmx).
Dados brutos: [`perf/resultados/rodada-01.jtl`](../../perf/resultados/rodada-01.jtl) (versionado — cada número abaixo sai dele).
Relatório HTML do JMeter gerado em `perf/relatorio-01/` (não versionado).

## Ambiente
| Item | Valor |
|---|---|
| Data | 2026-11-01 |
| Versão da aplicação | commit `644787f` (branch `fase-7/carga-mutacao`), Spring Boot 3.5.16, Java 21 |
| Banco | PostgreSQL 17 em container (`compose.yaml`), banco recém-criado com os dados de demonstração |
| Máquina | notebook Intel Core i7-11390H, 8 núcleos lógicos, 15,7 GB de RAM, Windows 11 |
| Gerador de carga | Apache JMeter 5.6.3, modo CLI, **na mesma máquina** da aplicação |
| Comando | `jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01` |

## Carga aplicada
50 usuários (threads), ramp-up 10 s, 70 s no total; login uma vez por usuário; 70% consulta de disponibilidade e
30% criação de reserva da **Sala 101, 2026-12-01 08:00–10:00 (UTC)** — o mesmo recurso e horário para todos.
Janela medida: 70,0 s · **18.899 pedidos** · ~268,6 pedidos/s no total.

## Resultados (calculados do `.jtl`)
| Operação | Pedidos | p50 | p95 | p99 | Máx. | Vazão | Códigos | Erro real |
|---|---|---|---|---|---|---|---|---|
| Login | 50 | 351 ms | 568 ms | 585 ms | 585 ms | 0,7/s | 200 = 50 | 0 (0,00%) |
| Consulta de disponibilidade | 13.197 | 125 ms | **332 ms** | 528 ms | 1.538 ms | 188,7/s | 200 = 13.197 | 0 (0,00%) |
| Criação de reserva (mesmo recurso) | 5.652 | 195 ms | **506 ms** | 795 ms | 1.648 ms | 80,8/s | 201 = 1 · 409 = 5.651 | 0 (0,00%) |

**409 não foi contado como erro**: é a recusa correta na disputa pelo mesmo recurso. Erro real = qualquer outra
resposta ou falha de asserção — houve **zero**.

## Integridade conferida no banco (RN-03 sob carga)
Consulta ao PostgreSQL logo após a rodada, antes de desligar:
- `reserva` da Sala 101 no intervalo disputado: **1 linha, estado APROVADA**.
- `tentativa_recusada` do mesmo recurso e horário: **5.651 linhas** — exatamente o número de respostas 409.

## Decisão
| Requisito | Meta | Medido | Decisão |
|---|---|---|---|
| RNF-01 consulta de disponibilidade | p95 ≤ 300 ms, erro < 1% | p95 = 332 ms; erro 0,00% | **não atende** (p95 32 ms acima) |
| RNF-02 criação de reserva | p95 ≤ 300 ms; só 201/409; erro < 1% | p95 = 506 ms; só 201/409; erro 0,00% | **não atende** no tempo · **atende** em "nunca 500" |
| RNF-03 integridade sob carga | no máximo 1 aceita | 1 aceita | **atende** |

Registrado como defeito de desempenho: [DEF-003](../defeitos/DEF-003-p95-acima-da-meta-sob-carga.md).

## Hipótese de gargalo (com o dado que a sustenta)
1. **Fila na trava do recurso:** toda criação disputa a mesma linha (`SELECT … FOR UPDATE`, ADR-002), então as
   criações são atendidas uma por vez. Dado: a criação tem p95 (506 ms) bem acima da consulta (332 ms), embora
   faça menos trabalho de leitura.
2. **Transação extra por recusa:** cada 409 abre uma segunda transação para registrar o conflito evitado.
   Dado: 5.651 registros gravados em 70 s.
3. **Pool de conexões padrão (10) para 50 usuários:** pedidos esperam conexão livre. Dado indireto: a consulta,
   que não trava nada, também passa de 300 ms no p95. *Não medido diretamente nesta rodada.*
4. **Gerador e aplicação na mesma máquina** disputando CPU.

## Próximo experimento
Medir o tempo de espera por conexão do pool (métrica do HikariCP) e repetir a rodada com o JMeter em outra máquina;
se a espera por conexão dominar, testar pool de 20; se a fila da trava dominar, avaliar alternativa no ADR-002
(condição de revisão já prevista lá). Uma mudança por rodada, cada uma com o próprio `resultado-NN.md`.

## Limites desta conclusão
- Máquina local de desenvolvimento, com o gerador de carga junto: **não permite afirmar nada sobre produção**.
- Uma única rodada; sem repetição para medir variação entre rodadas.
- Cenário de pior caso deliberado (todos disputando o mesmo recurso, sem pausa entre pedidos); uso real distribui as reservas por vários recursos.
