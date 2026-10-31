# ADR-004 — Intervalos semiabertos [início, término)

Status: aceita (2026-10-31, Guilherme Viana Lopes) · decisão D-05

## Contexto e forças
- A especificação diz que reservas do mesmo recurso não podem se sobrepor, mas não define se reservas "encostadas" (uma termina às 10h, outra começa às 10h) conflitam.
- Na grade real, horários encostados são o caso mais comum (aula das 8h às 10h seguida de outra das 10h às 12h).
- A fronteira é ponto de sensibilidade: o professor usa "`<` no lugar de `<=` na sobreposição" como exemplo de defeito sutil (Aula 16).

## Requisitos e cenários
RN-01, RN-02, RN-04 · CEN-06.

## Alternativas
- (a) Intervalo fechado `[início, término]`: reservas encostadas conflitariam e obrigariam intervalos artificiais.
- (b) **Semiaberto `[início, término)`**: inclui o início, exclui o término.

## Decisão
(b). Duas reservas A e B do mesmo recurso se sobrepõem quando `A.inicio < B.termino` **e** `B.inicio < A.termino`. O mesmo critério vale para manutenção. No banco: `tstzrange(inicio, termino, '[)')`.

## Consequências
- \+ Reservas encostadas são aceitas; o sistema reflete a grade real.
- − Exige testes de fronteira explícitos para não regredir.

## Evidência necessária
- Trinca de limites (−1 s, 0, +1 s) para início e fim, em sobreposição e em manutenção (Aulas 07 e 09).
- Mutação manual trocando `<` por `<=` deve derrubar pelo menos um teste (Aula 12).

## Condição de revisão
Se a instituição exigir intervalo mínimo entre reservas (ex.: 10 minutos para limpeza), registrar como nova regra.
