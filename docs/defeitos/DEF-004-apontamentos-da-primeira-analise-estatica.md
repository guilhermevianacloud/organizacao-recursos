# DEF-004 — Apontamentos da primeira análise estática (SonarCloud)

| Campo | Valor |
|---|---|
| Identificador | DEF-004 |
| Situação | corrigido (11 de 12); 1 mantido com justificativa (ADR-007, proposta) |
| Requisito afetado | RNF-10 ("0 bugs ou vulnerabilidades críticas") · RNF-05 · RF-13 (mensagens compreensíveis na interface) |
| Encontrado por | SonarCloud, primeira análise (automática, commit `883fd8f`) |
| Data | 2026-11-01 |

## Resumo observável
A primeira análise do SonarCloud apontou 12 problemas: Security D (1) e Reliability D (11). Dois são de severidade alta:
- `java:S4502`: CSRF desligado em `ConfiguracaoDeSeguranca`.
- `java:S2119`: `new SecureRandom()` criado a cada chamada em `ConfiguracaoDeSeguranca`.

Os outros 10 são de severidade média: 8× `javascript:S9383` (promessas sem tratamento de rejeição em `static/js/app.js`) e 2× `java:S8786` (regex com backtracking em `ApiDeReservasIT`).

## Ambiente / versão
`main` em `883fd8f` (depois do PR #9), análise automática do SonarCloud, sem cobertura.

## Passos mínimos para reproduzir
1. Abrir o projeto `guilhermevianacloud_organizacao-recursos` no SonarCloud, na análise do commit `883fd8f`.
2. Ver a aba Issues.

## Resultado obtido
12 issues abertas, 2 de severidade alta. A meta "0 bugs ou vulnerabilidades críticas" não era atendida.

## Resultado esperado / requisito
0 bugs ou vulnerabilidades críticas (especificação, metas mensuráveis).

## Severidade (com justificativa)
**Alta.** Conta para uma meta da especificação. Nenhum dos apontamentos causava falha observada nos 273 testes. Na interface, porém, uma tela que falhasse ao carregar podia ficar sem mensagem de erro (S9383).

## Prioridade (com justificativa)
**P1.** Precisa estar resolvido antes da versão avaliada; as correções são pequenas.

## Causa raiz (5 porquês)
1. Por que os apontamentos chegaram até a `main`? Porque nenhuma análise estática rodava no CI.
2. Por que não rodava? Porque o passo do SonarCloud dependia do `SONAR_TOKEN`, ainda não configurado.
3. Por que os testes não pegaram? Porque testes conferem comportamento; estes apontamentos são de forma (promessa sem tratamento, objeto recriado, regex lenta) e não mudavam o resultado nos cenários testados.
4. Por que a interface tinha promessas sem tratamento? Porque as telas eram abertas a partir de eventos sem `await`, e o tratamento de erro ficava só na abertura da seção.
5. Condição controlável: **análise estática fora do pipeline.** Agora roda em todo PR e na `main`.

## Correção
- S2119: `SecureRandom` num campo estático, reaproveitado.
- S4502: CSRF continua desligado. A API é stateless, o token vai no cabeçalho `Authorization` e não há cookie, então não existe envio automático de credencial para explorar. A justificativa está no código e no [ADR-007](../adr/ADR-007-csrf-desligado.md), que é uma **proposta**. A marcação como revisado no SonarCloud depende do Guilherme.
- S9383: dentro de `try`, `await`; nas chamadas disparadas por evento, `emSegundoPlano()`, que mostra o erro na tela.
- S8786: a leitura do JSON nos testes usa `JsonPath` em vez de regex.

## Critério de verificação
PR #10: CI verde (98 + 175) e análise do SonarCloud pelo CI com **Quality Gate aprovado**, 0 issues novas, cobertura 98,0% / 95,0% e 0% de duplicação. Os números da `main` depois do merge ficam em `docs/evidencias/README.md`.
