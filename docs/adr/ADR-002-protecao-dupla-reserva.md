# ADR-002 — Proteção contra dupla reserva no banco

Status: aceita (2026-10-31, Guilherme Viana Lopes) · decisão D-06

## Contexto e forças
- Regra crítica da especificação: "duas solicitações simultâneas devem produzir uma única reserva aceita" (RN-03). Possibilidade de dupla reserva é **limitador de nota**.
- Verificar "o recurso está livre?" e depois gravar são dois passos: duas requisições simultâneas podem ver "livre" e gravar as duas (condição de corrida).
- `synchronized` ou lock em memória protegem só uma instância da aplicação, não várias.
- A Aula 10 orienta corrigir no banco, com "constraint de exclusão do PostgreSQL (EXCLUDE USING gist sobre tstzrange) ou lock pessimista".

## Requisitos e cenários
RF-05, RF-06, RN-02, RN-03 · cenário CEN-01 · driver DR-01.

## Alternativas
- (a) Checagem na aplicação + `synchronized`: não protege várias instâncias. Descartada.
- (b) Lock pessimista (`SELECT … FOR UPDATE`) na linha do recurso: funciona, mas depende de todo caminho de escrita lembrar de travar.
- (c) **Exclusion constraint** no PostgreSQL: o banco recusa gravar duas linhas do mesmo recurso com intervalos (`tstzrange`) que se cruzam.

## Decisão
(c) Exclusion constraint com índice GiST, combinando igualdade do recurso (extensão `btree_gist`) e sobreposição do intervalo `tstzrange(inicio, termino, '[)')`, **somente para estados que ocupam o recurso** (SOLICITADA, APROVADA, EM_USO). A violação é traduzida em **HTTP 409 Conflict** com ProblemDetail e registrada como conflito evitado.
A aplicação também verifica antes de gravar, só para dar uma mensagem melhor; quem decide é o banco.

## Consequências
- \+ Vale para qualquer número de instâncias e para qualquer caminho de escrita, inclusive SQL direto.
- \+ Um único mecanismo protege sala, material e professor (recurso único com tipo).
- − Prende o projeto ao PostgreSQL (não roda em H2, o que já é exigência de teste).
- − O índice GiST tem custo de escrita e o bloqueio reduz a vazão sob disputa (ponto de trade-off integridade × throughput).

## Riscos
- R-01: se a condição de estados estiver errada, reservas canceladas continuariam bloqueando o recurso.

## Evidência necessária
- Teste de integração (Testcontainers): 20 pedidos simultâneos → 1 aceito, 19 × 409, 1 linha no banco; repetido 5 vezes; vermelho sem a constraint e verde com ela (Aula 10).
- Mutação manual: remover a constraint e ver o teste falhar (Aula 12).

## Condição de revisão
Se o teste de carga mostrar p95 acima do RNF-02 por causa da constraint, avaliar lock pessimista em novo ADR.
