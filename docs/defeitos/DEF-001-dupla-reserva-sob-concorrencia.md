# DEF-001 — Dupla reserva aceita sob pedidos simultâneos

| Campo | Valor |
|---|---|
| Identificador | DEF-001 |
| Situação | corrigido (ver "Correção" e DEF-002) |
| Requisito afetado | RF-06 / RN-03 (crítico, limitador de nota) · também RF-05 / RN-02 na agenda do professor |
| Encontrado por | `DuplaReservaConcorrenciaIT` (teste de concorrência, Aula 10) |
| Data | 2026-10-31 |

## Resumo observável
Com 20 pedidos simultâneos para o mesmo recurso e o mesmo horário, o sistema aceitou várias reservas em vez de uma.

## Ambiente / versão
Branch `fase-3/nucleo-reservas`, commit do teste vermelho (`test(red): RN-03 …`); PostgreSQL 17 via Testcontainers; Windows 11, JDK 21.

## Pré-condições
Sala 101 (não restrita) sem reservas; um Solicitante cadastrado.

## Passos mínimos para reproduzir
1. Disparar 20 chamadas a `ServicoDeReservas.solicitar` para a sala 101, 2026-11-09 08:00–10:00, liberadas juntas por um `CountDownLatch`.
2. Contar as reservas ativas da sala no banco.

## Resultado obtido
| Execução | Reservas aceitas para a mesma sala e horário |
|---|---|
| 1 | 10 |
| 2 | 7 |
| 3 | 6 |
| 4 | 4 |
| 5 | 6 |
| Agenda do professor (mesmo teste) | 4 |

## Resultado esperado / requisito
Exatamente 1 reserva aceita, 19 recusadas com conflito, 1 linha no banco (RN-03; Aula 10).

## Evidências sanitizadas
Relatório do Failsafe: `expected: 1 but was: 10` (e demais valores acima) na asserção "pedidos aceitos". Nenhum dado pessoal real (usuário `marina@exemplo.invalid`).

## Frequência, alcance e impacto
Sempre que pedidos pelo mesmo recurso chegam ao mesmo tempo (pico, semana de matrícula). Duas turmas na mesma sala; professor em dois lugares ao mesmo tempo.

## Severidade (com justificativa)
**Crítica** — viola a integridade da regra central do sistema e é limitador de nota explícito da especificação.

## Prioridade (com justificativa)
**P0** — sem esta correção nenhuma outra funcionalidade de reserva é confiável; bloqueia a entrega.

## Causa raiz (5 porquês)
1. Por que várias reservas foram aceitas? Porque cada pedido verificou "o recurso está livre?" e encontrou livre.
2. Por que todos encontraram livre? Porque fizeram a consulta antes de qualquer um gravar.
3. Por que a consulta e a gravação não foram atômicas? Porque a regra estava só na aplicação, em dois passos separados (consultar, depois inserir).
4. Por que a regra estava só na aplicação? Porque a invariante "sem sobreposição" não estava declarada no banco, o único ponto que todos os pedidos compartilham.
5. Condição controlável: **regra crítica de integridade implementada sem proteção no banco.**

## Correção
- Migração `V3` com **exclusion constraint** (`btree_gist`, `tstzrange(inicio, termino, '[)')`, só para estados que ocupam o recurso) — decisão já registrada no [ADR-002](../adr/ADR-002-protecao-dupla-reserva.md).
- `ServicoDeReservas` traduz a violação da constraint em `ConflitoDeHorarioException` (409 na API, Fase 4).
- Verificação: o mesmo teste, sem alteração, ficou verde (ver `docs/testes/evidencia-tdd.md`).

## Critério de verificação
`DuplaReservaConcorrenciaIT` verde nas 5 repetições e no caso do professor; mutação manual (remover a constraint) faz o teste voltar a falhar.

## Rastreio (Aula 05: issue, commit, PR e teste de regressão)
| Item | Valor |
|---|---|
| Issue | [#11](https://github.com/guilhermevianacloud/organizacao-recursos/issues/11) (fechada) |
| Commit vermelho | `2f6b515` test(red): RN-03 — 20 pedidos simultâneos pelo mesmo recurso |
| Commit da correção | `70dcfff` feat(green): RN-03 — constraint no banco e trava por recurso (DEF-001, DEF-002) |
| Pull request | [#3](https://github.com/guilhermevianacloud/organizacao-recursos/pull/3) |
| Teste de regressão | `servico/DuplaReservaConcorrenciaIT`, `servico/ProtecaoNoBancoIT` |
| Verde reproduzível | [CI run 41348450251](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41348450251) (`main`, `16adf58`) |

A correção usou a convenção de TDD da Aula 09 (`test(red)` / `feat(green)`), anterior à convenção `fix(<id>)` da Aula 15.
