# Requisitos de desempenho (proposta para o grupo validar)

Aula 10. Escrito **antes** da primeira rodada de carga (o commit deste arquivo vem antes do plano e do resultado),
para a meta não ser ajustada ao número medido. Os valores seguem o exemplo da aula e são **proposta** do grupo,
não exigência da especificação. Status: **proposta — validação pendente pelo grupo**.

## Requisitos
| ID | Operação | Medida | Origem |
|---|---|---|---|
| RNF-01 | Consulta de disponibilidade (`GET /api/recursos` com filtro de horário) | **p95 ≤ 300 ms** com **50 usuários simultâneos**; **erro < 1%** | PRD, exemplo da Aula 10 |
| RNF-02 | Criação de reserva disputando o mesmo recurso (`POST /api/reservas`) | **p95 ≤ 300 ms** com 50 usuários; só responde **201 ou 409, nunca 500**; erro real < 1% | PRD, exemplo da Aula 10 |
| RNF-03 (integridade sob carga) | Mesmo recurso e horário disputados durante toda a rodada | **no máximo uma** reserva aceita por recurso e intervalo, conferido no banco | RN-03 |

**409 não é erro** nesta medição: na disputa pelo mesmo recurso, recusar é o comportamento correto. Erro real é
qualquer outra resposta (5xx, falha de conexão, 401 inesperado).

## Modelo de carga
| Parâmetro | Valor |
|---|---|
| Usuários simultâneos (threads) | 50 |
| Ramp-up | 10 s |
| Duração | 60 s (depois do ramp-up, todos ativos) |
| Login | uma vez por usuário, token extraído do JSON da resposta |
| Mistura de operações | 70% consulta de disponibilidade · 30% criação de reserva |
| Recurso disputado | o mesmo para todos (Sala 101 dos dados de demonstração), mesmo intervalo |
| Pausa entre pedidos | nenhuma (pior caso para a disputa) |
| Ambiente | aplicação local (`./mvnw spring-boot:run`) com PostgreSQL em container — **nunca** ambiente compartilhado |

## Limites desta medição
- Máquina local de desenvolvimento: o resultado **não prova** desempenho em produção.
- Gerador de carga (JMeter) e aplicação na mesma máquina: disputam CPU; o número tende a ser pior que o real.
- Banco recém-criado, com poucos dados.
