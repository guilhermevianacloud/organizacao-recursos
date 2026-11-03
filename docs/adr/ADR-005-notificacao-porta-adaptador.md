# ADR-005 — Notificação por porta e adaptador HTTP

Status: aceita (2026-10-31, Guilherme Viana Lopes) · decisão D-13

## Contexto e forças
- RF-11: "notificação simulada ou integração com API externa".
- A estratégia obrigatória da especificação exige "WireMock para integrações externas".
- Aula 08: só o adaptador HTTP conhece URL, token e formato externo; o domínio conhece só uma interface; a falha externa não pode controlar a suíte nem vazar segredo.

## Requisitos e cenários
RF-11, RN-13, RNF-06, RNF-07 · CEN-05.

## Alternativas
- (a) Notificação só simulada (log interno): cumpre RF-11, mas deixa sem uso a exigência de WireMock.
- (b) **Porta `Notificador` + adaptador HTTP** para um provedor externo simulado, testado com WireMock.

## Decisão
(b). O envio acontece **depois do commit** da mudança de estado, com timeout de 2 s; a falha do provedor é registrada e não desfaz a reserva (RN-13). URL e credencial vêm de configuração externa; os logs não registram cabeçalho de autorização nem corpo sensível.

## Consequências
- \+ Cumpre RF-11 e a exigência de WireMock da especificação.
- \+ Trocar de provedor muda só o adaptador.
- − Mais uma peça para manter; o dublê do WireMock pode ficar desatualizado em relação a um provedor real.

## Riscos
- O dublê do WireMock pode divergir do provedor real, que não existe neste projeto → o contrato está nos testes INT-01..06; um provedor real exigiria testá-los contra ele.
- Notificação perdida quando o provedor falha (não há nova tentativa) → a reserva não depende dela; a falha é registrada em log, sem a chave.

## Evidência necessária
Matriz de falhas da Aula 08 com WireMock: INT-01 200 válido · INT-02 404 · INT-03 500 com reserva mantida · INT-04 timeout · INT-05 JSON malformado · INT-06 cabeçalho presente e segredo ausente do log.

## Condição de revisão
Se surgir um provedor real, adicionar verificação periódica de contrato contra ele.
