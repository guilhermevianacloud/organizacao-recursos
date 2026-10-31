# DEF-002 — Deadlock na disputa pelo mesmo recurso vira erro inesperado

| Campo | Valor |
|---|---|
| Identificador | DEF-002 |
| Situação | corrigido |
| Requisito afetado | RF-06 / RN-03 · RNF-02 ("criação só responde 201 ou 409, nunca 500") |
| Encontrado por | `DuplaReservaConcorrenciaIT` (asserção "exceções inesperadas") |
| Data | 2026-10-31 |

## Resumo observável
Com a exclusion constraint da correção do DEF-001 e sem nenhuma outra proteção, nunca houve reserva duplicada. Porém, de vez em quando, um dos 20 pedidos simultâneos terminava com `CannotAcquireLockException: deadlock detected`, em vez de ser recusado como conflito.

## Ambiente / versão
Branch `fase-3/nucleo-reservas`, migração V3 aplicada, serviço ainda buscando o recurso sem trava; PostgreSQL 17 (Testcontainers).

## Passos mínimos para reproduzir
1. Rodar `./mvnw -B verify` (o `DuplaReservaConcorrenciaIT` dispara 20 pedidos simultâneos pela mesma sala, 5 vezes).
2. Observar o relatório do Failsafe.

## Resultado obtido
2 falhas em 7 testes (uma delas na 4ª repetição da sala), com tempo de 19 s no teste afetado: "Expecting empty but was: [CannotAcquireLockException … ERROR: deadlock detected]". Intermitente: depende de como as transações se intercalam.

## Resultado esperado / requisito
Cada pedido termina aceito ou recusado com conflito (409). Nenhum erro inesperado (seria um 500 na API).

## Severidade (com justificativa)
**Alta** — a integridade foi mantida (sem duplicata), mas o usuário receberia erro 500 em vez de uma resposta compreensível, e o teste de carga da Aula 10 exige "nunca 500".

## Prioridade (com justificativa)
**P1** — aparece justamente no pico de uso; precisa estar corrigido antes do teste de carga e da entrega, mas não compromete dados.

## Causa raiz (5 porquês)
1. Por que surgiu um erro inesperado? Porque o PostgreSQL abortou uma transação por deadlock.
2. Por que houve deadlock? Porque várias inserções do mesmo recurso esperavam umas pelas outras na verificação da exclusion constraint.
3. Por que elas esperavam umas pelas outras? Porque todas chegavam ao INSERT ao mesmo tempo, cada uma segurando a própria linha provisória.
4. Por que todas chegavam ao INSERT ao mesmo tempo? Porque nada ordenava os pedidos pelo mesmo recurso antes da escrita.
5. Condição controlável: **escrita concorrente no mesmo recurso sem ponto de serialização.** (A constraint garante o resultado, mas não organiza a fila.)

## Correção
- `RecursoRepository.buscarTravandoParaReserva` (`SELECT … FOR UPDATE`): o serviço trava a linha do recurso antes de verificar e gravar. Pedidos pelo mesmo recurso passam a ser atendidos um por vez; recursos diferentes continuam em paralelo.
- A exclusion constraint continua como garantia final ([ADR-002](../adr/ADR-002-protecao-dupla-reserva.md), atualizado) e tem teste próprio que grava direto com SQL (`ProtecaoNoBancoIT`).
- Isolamento de teste corrigido no mesmo ciclo: o teste de concorrência agora limpa o banco também ao terminar.

## Critério de verificação
`./mvnw -B verify` três vezes seguidas: 85 de unidade e 28 de integração, 0 falhas e 0 erros nas três (15 disputas de 20 pedidos pela sala + professor).
