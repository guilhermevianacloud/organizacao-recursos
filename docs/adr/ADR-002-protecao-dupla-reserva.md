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
**(c) + (b), em duas camadas** (revisada em 2026-10-31 após o DEF-002):
1. **Exclusion constraint** com índice GiST, combinando igualdade do recurso (extensão `btree_gist`) e sobreposição do intervalo `tstzrange(inicio, termino, '[)')`, **somente para estados que ocupam o recurso** (SOLICITADA, APROVADA, EM_USO). É a **garantia**: vale para qualquer caminho de escrita, inclusive SQL direto.
2. **Lock pessimista na linha do recurso** (`SELECT … FOR UPDATE`) antes de verificar e gravar. É a **organização da fila**: pedidos pelo mesmo recurso são atendidos um por vez, a verificação da aplicação enxerga a reserva de quem chegou antes e devolve conflito limpo.

O conflito é traduzido em `ConflitoDeHorarioException` (HTTP 409 com ProblemDetail na API).

### Por que as duas camadas
| Configuração testada | Resultado observado (`DuplaReservaConcorrenciaIT`) |
|---|---|
| Só verificação na aplicação | 10, 7, 6, 4 e 6 reservas aceitas para a mesma sala (DEF-001) |
| Constraint, sem lock | nenhuma duplicata, mas deadlock intermitente → erro inesperado, que seria 500 (DEF-002) |
| **Constraint + lock** | 1 aceita e 19 conflitos em todas as execuções (3 rodadas completas × 5 repetições) |

## Consequências
- \+ A regra vale para qualquer número de instâncias e para qualquer caminho de escrita.
- \+ Um único mecanismo protege sala, material e professor (recurso único com tipo).
- \+ Sob disputa, todo pedido termina em aceito ou 409, sem erro 500.
- − Prende o projeto ao PostgreSQL (não roda em H2, o que já é exigência de teste).
- − Pedidos pelo **mesmo** recurso esperam a vez (ponto de trade-off integridade × throughput); recursos diferentes seguem em paralelo.
- − O índice GiST tem custo de escrita.

## Riscos
- R-01: se a condição de estados estiver errada, reservas canceladas continuariam bloqueando o recurso (coberto por `ProtecaoNoBancoIT`).
- Todo caminho de escrita de reserva precisa usar a busca com trava; um caminho novo sem ela ainda fica protegido pela constraint, mas pode voltar a ter deadlock.

## Evidência
- `DuplaReservaConcorrenciaIT`: 20 pedidos simultâneos → 1 aceito, 19 conflitos, 1 linha no banco; 5 repetições + agenda do professor; vermelho sem proteção (commit `test(red): RN-03`) e verde com ela.
- `ProtecaoNoBancoIT`: a constraint recusa sobreposição gravada direto com SQL, aceita reserva encostada e ignora cancelada.
- Mutação manual (Aula 12): remover a constraint e a trava deve derrubar o teste de concorrência.

## Condição de revisão
Se o teste de carga mostrar p95 acima do RNF-02 por causa da fila por recurso, avaliar alternativas (por exemplo, retentativa em deadlock sem trava) em novo ADR.
