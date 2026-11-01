# Cenários BDD — aprovação de restrito e bloqueio por manutenção

Aula 09. Cenários Dado/Quando/Então das quatro regras: recurso restrito exige aprovação de um Responsável
(RN-05); o Solicitante não aprova a própria reserva (RN-06); recurso em manutenção não pode ser reservado
(RN-04); a reserva só muda de estado pelas transições permitidas (RN-07).
Especificação executável: [`src/test/resources/features/aprovacao-bloqueio.feature`](../../src/test/resources/features/aprovacao-bloqueio.feature),
rodada no `./mvnw -B verify` por `CucumberIT` (Cucumber-JVM, passos com MockMvc e token real de cada perfil).

> **Ordem honesta:** estas regras foram implementadas por TDD nas Fases 3 e 4 (`docs/testes/evidencia-tdd.md`).
> Os cenários foram escritos depois, como especificação em linguagem de negócio das mesmas regras, e passaram
> na primeira execução. A prova de que eles detectam a quebra da regra fica na mutação manual (Fase 7).

## 1. Recurso restrito exige aprovação de um Responsável (RN-05)
- **Dado** um recurso restrito **Quando** o Solicitante pede o recurso das 08:00 às 10:00 **Então** a resposta é 201 **e** a reserva fica SOLICITADA.
- **Dado** uma reserva de recurso restrito aguardando aprovação **Quando** o Responsável aprova **Então** a resposta é 200 **e** a reserva fica APROVADA.

## 2. Solicitante não aprova a própria reserva (RN-06)
- **Dado** uma reserva de recurso restrito aguardando aprovação **Quando** o próprio Solicitante tenta aprovar **Então** a resposta é 403 **e** a reserva continua SOLICITADA.

## 3. Recurso em manutenção não pode ser reservado (RN-04)
Manutenção da sala: 13:00 → 17:00. Tabela de exemplos com as fronteiras do período:

| Pedido | Situação | Esperado |
|---|---|---|
| 11:00 → 13:00 | termina exatamente no início | 201 (intervalo semiaberto, ADR-004) |
| 11:00 → 13:00:01 | cruza um segundo no início | 409 |
| 17:00 → 18:00 | começa exatamente no fim | 201 |
| 16:59:59 → 18:00 | cruza um segundo no fim | 409 |
| 14:00 → 15:00 | dentro | 409 |

## 4. Só transições permitidas (RN-07)
| Estado atual | Ação do Responsável | Esperado |
|---|---|---|
| APROVADA | retirada | 200 |
| EM_USO | devolução | 200 |
| SOLICITADA | retirada | 409 |
| CANCELADA | aprovação | 409 |
| CONCLUIDA | retirada | 409 |

## Perguntas em aberto
- Q-02: manutenção cadastrada sobre reservas existentes — comportamento provisório: o cadastro é recusado.
- O PRD decidiu as fronteiras (ADR-004); nenhuma política foi inventada nos cenários.

## Unidade × API
| Cenário | Nível |
|---|---|
| Fronteiras de intervalo e de manutenção | unidade (`IntervaloTest`) + integração (`ServicoDeReservasIT`) + API (feature) |
| Transições permitidas e proibidas | unidade (`EstadoReservaTest`, 49 pares) + API (feature) |
| Aprovação por perfil e RN-06 | API (feature e `MatrizDeAutorizacaoIT`) — depende do token |
