# Plano TDD — inventário das regras de negócio

Aula 05 (Prompt 1). Fonte das regras: [`docs/prd.md`](../prd.md) (seção 8) e [`docs/arquitetura.md`](../arquitetura.md). Nenhuma regra foi inventada: cada uma cita sua origem.
Categorias de caso: **F** caminho feliz · **L** valor-limite · **I** entrada inválida · **C** conflito · **P** estado proibido.
Convenção de horários: dia fixo 2026-11-09, fuso UTC nos testes (`Clock.fixed`).

## 1. Inventário de casos

### RN-01 — O término deve ser posterior ao início
| Caso | Cat. | Entrada | Resultado esperado |
|---|---|---|---|
| RN01-01 | F | 08:00 → 10:00 | aceita |
| RN01-02 | L | 08:00 → 08:00:01 (1 s de duração) | aceita |
| RN01-03 | L | 08:00 → 08:00 (duração zero) | 400, "o término deve ser posterior ao início" |
| RN01-04 | I | 10:00 → 08:00 | 400, mesma mensagem |
| RN01-05 | I | início ou término ausente | 400, campo obrigatório |

### RN-02 — Sem sobreposição de reservas ativas do mesmo recurso
Reserva existente A: sala 101, 08:00 → 10:00, APROVADA.
| Caso | Cat. | Nova reserva | Resultado esperado |
|---|---|---|---|
| RN02-01 | F | sala 101, 10:00 → 12:00 (encostada no fim) | aceita (ADR-004) |
| RN02-02 | L | sala 101, 09:59:59 → 12:00 (cruza 1 s) | 409 |
| RN02-03 | L | sala 101, 10:00:01 → 12:00 | aceita |
| RN02-04 | F | sala 101, 06:00 → 08:00 (encostada no início) | aceita |
| RN02-05 | L | sala 101, 06:00 → 08:00:01 | 409 |
| RN02-06 | C | sala 101, 08:30 → 09:30 (dentro de A) | 409 |
| RN02-07 | C | sala 101, 07:00 → 11:00 (contém A) | 409 |
| RN02-08 | F | **sala 102**, 08:00 → 10:00 (outro recurso) | aceita |
| RN02-09 | F | sala 101, 08:00 → 10:00, com A **CANCELADA** | aceita (cancelada não ocupa) |
| RN02-10 | C | **professor** P1 já alocado 08:00 → 10:00; nova alocação de P1 09:00 → 11:00 | 409 (agenda do professor) |
| RN02-11 | C | **material** M1 já reservado; mesmo horário | 409 |

### RN-03 — Solicitações simultâneas geram uma única reserva aceita
| Caso | Cat. | Entrada | Resultado esperado |
|---|---|---|---|
| RN03-01 | C | 20 pedidos simultâneos, sala 101, 08:00 → 10:00 | exatamente 1 × 201, 19 × 409; **1 linha no banco** (consulta) |
| RN03-02 | C | 20 pedidos simultâneos, mesmo professor e horário | idem |
| RN03-03 | C | RN03-01 repetido 5 vezes seguidas | 5 de 5 com o mesmo resultado |
| RN03-04 | F | 20 pedidos simultâneos para 20 salas diferentes | 20 × 201 |

### RN-04 — Recurso em manutenção não pode ser reservado
Manutenção da sala 101: 13:00 → 17:00.
| Caso | Cat. | Nova reserva | Resultado esperado |
|---|---|---|---|
| RN04-01 | F | 11:00 → 13:00 (termina exatamente no início) | aceita |
| RN04-02 | L | 11:00 → 13:00:01 (cruza 1 s) | recusada, "recurso em manutenção" |
| RN04-03 | F | 17:00 → 18:00 (começa exatamente no fim) | aceita |
| RN04-04 | L | 16:59:59 → 18:00 | recusada |
| RN04-05 | C | 14:00 → 15:00 (dentro) | recusada |
| RN04-06 | F | sala 102 no mesmo horário | aceita |
| RN04-07 | C | cadastrar manutenção sobre reserva ativa existente | recusado (Q-02, comportamento provisório) |

### RN-05 — Somente o Responsável aprova recurso restrito
| Caso | Cat. | Quem aprova | Resultado esperado |
|---|---|---|---|
| RN05-01 | F | Responsável | 200, estado APROVADA, auditoria registrada |
| RN05-02 | P | Solicitante | 403, estado continua SOLICITADA |
| RN05-03 | P | Administrador | 403 (D-09) |
| RN05-04 | P | anônimo | 401 |
| RN05-05 | F | reserva de recurso **não** restrito | nasce APROVADA, sem aprovação manual (D-08) |

### RN-06 — O Solicitante não aprova a própria reserva
| Caso | Cat. | Situação | Resultado esperado |
|---|---|---|---|
| RN06-01 | P | usuário que pediu a reserva tenta aprová-la | 403 |
| RN06-02 | F | outro usuário Responsável aprova | 200 |

### RN-07 — Só transições permitidas
| Caso | Cat. | De → Para | Resultado esperado |
|---|---|---|---|
| RN07-01 | F | SOLICITADA → APROVADA | permitida |
| RN07-02 | F | SOLICITADA → REJEITADA | permitida |
| RN07-03 | F | SOLICITADA → CANCELADA | permitida |
| RN07-04 | F | APROVADA → CANCELADA | permitida |
| RN07-05 | F | APROVADA → EM_USO | permitida |
| RN07-06 | F | APROVADA → NAO_COMPARECEU | permitida |
| RN07-07 | F | EM_USO → CONCLUIDA | permitida |
| RN07-08 | P | SOLICITADA → EM_USO | recusada (409) |
| RN07-09 | P | CANCELADA → APROVADA | recusada |
| RN07-10 | P | CONCLUIDA → qualquer | recusada |
| RN07-11 | P | REJEITADA → qualquer | recusada |
| RN07-12 | P | NAO_COMPARECEU → qualquer | recusada |
| RN07-13 | P | EM_USO → CANCELADA | recusada |
A tabela completa (todos os pares) vira um `@ParameterizedTest`.

### RN-08 — Reserva iniciada não pode ser apagada
| Caso | Cat. | Estado | Resultado esperado |
|---|---|---|---|
| RN08-01 | P | EM_USO | exclusão recusada |
| RN08-02 | P | CONCLUIDA | exclusão recusada |
| RN08-03 | P | NAO_COMPARECEU | exclusão recusada |
| RN08-04 | F | API não oferece exclusão física: "cancelar" muda o estado | linha continua no banco com estado CANCELADA |

### RN-09 — Toda mudança de estado gera auditoria imutável, na mesma transação
| Caso | Cat. | Situação | Resultado esperado |
|---|---|---|---|
| RN09-01 | F | criar reserva | 1 registro: (nenhum) → SOLICITADA, com usuário e instante do `Clock` |
| RN09-02 | F | aprovar | registro SOLICITADA → APROVADA com motivo |
| RN09-03 | C | falha forçada após a mudança, antes do commit | nem mudança nem auditoria gravadas (0 órfãos) |
| RN09-04 | P | tentar alterar ou apagar registro de auditoria pela API | não existe rota; 404/405 |
| RN09-05 | P | Solicitante consulta histórico | 403 |
| RN09-06 | F | Responsável e Administrador consultam histórico | 200 |

### RN-10 — O Solicitante só altera e cancela as próprias reservas
| Caso | Cat. | Situação | Resultado esperado |
|---|---|---|---|
| RN10-01 | F | dono cancela a própria reserva | 200, CANCELADA |
| RN10-02 | P | outro Solicitante cancela pelo id (IDOR) | 403, reserva intacta |
| RN10-03 | P | outro Solicitante altera horário pelo id | 403, reserva intacta |
| RN10-04 | I | id inexistente | 404 |

### RN-11 — Canceladas e rejeitadas não contam como uso no relatório
| Caso | Cat. | Situação | Resultado esperado |
|---|---|---|---|
| RN11-01 | F | 2 reservas aprovadas de 2 h na sala 101 no período | 4 h para a sala 101 (conta no comentário do teste) |
| RN11-02 | F | + 1 reserva cancelada de 3 h | continua 4 h |
| RN11-03 | L | reserva de 09:00 → 13:00 com período terminando 11:00 | conta 2 h (D-14) |
| RN11-04 | F | período sem reservas | 0 h, lista vazia |
| RN11-05 | F | 3 pedidos recusados por sobreposição no período | conflitos evitados = 3 |
| RN11-06 | P | Solicitante pede o relatório | 403 |
| RN11-07 | P | sem token | 401 |

### RN-12 — Entrada inválida → 400 compreensível, sem detalhe interno
| Caso | Cat. | Entrada | Resultado esperado |
|---|---|---|---|
| RN12-01 | I | capacidade negativa no cadastro de sala | 400, mensagem em português |
| RN12-02 | I | campo obrigatório ausente | 400, indica o campo |
| RN12-03 | I | JSON malformado | 400 |
| RN12-04 | P | qualquer erro 4xx/5xx | corpo ProblemDetail **sem** stack trace, SQL ou nome de classe |

### RN-13 — Falha do provedor de notificação não desfaz a mudança
| Caso | Cat. | Resposta do provedor (WireMock) | Resultado esperado |
|---|---|---|---|
| RN13-01 | F | 200 válido (INT-01) | reserva gravada; notificação registrada como enviada |
| RN13-02 | I | 404 (INT-02) | reserva gravada; falha registrada |
| RN13-03 | I | 500 (INT-03) | reserva gravada; falha registrada |
| RN13-04 | L | atraso maior que 2 s (INT-04) | reserva gravada; envio interrompido no timeout |
| RN13-05 | I | JSON malformado (INT-05) | reserva gravada; erro controlado |
| RN13-06 | P | requisição ao provedor (INT-06) | cabeçalho obrigatório presente; segredo ausente do log |

## 2. Ambiguidades, lacunas e perguntas
- Q-02 (PRD): manutenção sobre reservas existentes — comportamento provisório: recusar o cadastro (RN04-07).
- Q-03: prazo para cancelar reserva aprovada — sem prazo por enquanto.
- Q-04: quando marcar NAO_COMPARECEU — manual, pelo Responsável.
- Artefatos procurados e ainda inexistentes: código de domínio e testes (começam na Fase 3).

## 3. Matriz regra → caso → teste → status
Preenchida na Fase 3 em diante: cada caso recebe o caminho e o nome do teste, e o status passa de **a escrever** para **vermelho** (fase red) e **verde** (fase green).

| Regra | Casos | Teste | Status |
|---|---|---|---|
| RN-01 | RN01-01..05 | — | a escrever |
| RN-02 | RN02-01..11 | — | a escrever |
| RN-03 | RN03-01..04 | — | a escrever |
| RN-04 | RN04-01..07 | — | a escrever |
| RN-05 | RN05-01..05 | — | a escrever |
| RN-06 | RN06-01..02 | — | a escrever |
| RN-07 | RN07-01..13 | — | a escrever |
| RN-08 | RN08-01..04 | — | a escrever |
| RN-09 | RN09-01..06 | — | a escrever |
| RN-10 | RN10-01..04 | — | a escrever |
| RN-11 | RN11-01..07 | — | a escrever |
| RN-12 | RN12-01..04 | — | a escrever |
| RN-13 | RN13-01..06 | — | a escrever |

**Regras sem teste:** todas, por enquanto (nenhum código de domínio escrito).
