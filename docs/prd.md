# Organização de Recursos — PRD

> **Fonte de verdade:** [Especificação do Projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html) (prof. Afonso Lelis, Qualidade de Software 2026.2).
> Tudo que vem da especificação está marcado **[Espec.]**. O que foi proposto pelo grupo está marcado **[Proposta]** e pode ser revisto com ADR. Nada aqui foi inventado como se fosse exigência.

## 1. Visão do produto
Um sistema confiável para alocar **salas, professores e materiais sem conflitos** de horário **[Espec.]**, em que cada reserva aceita é garantida — mesmo quando duas pessoas pedem o mesmo recurso no mesmo instante — e cada mudança fica registrada.

O foco não é só funcionar: é apresentar **evidências objetivas** de qualidade, segurança, rastreabilidade e desempenho **[Espec.]**.

## 2. Problema
Reservas feitas em planilhas, e-mails ou sistemas sem controle de concorrência geram:
- a mesma sala, material ou professor alocado duas vezes no mesmo horário;
- reservas em recursos que estão em manutenção;
- recursos restritos (laboratórios, equipamentos caros) usados sem aprovação;
- nenhum histórico de quem mudou o quê, impossibilitando auditar conflitos.

## 3. Personas **[Proposta]**
Derivadas dos três perfis da especificação. Nomes fictícios.

### P1 — Marina, professora (perfil **Solicitante**)
- **Objetivo:** reservar a sala certa e o projetor para a aula de quinta em menos de 2 minutos, com a certeza de que a reserva vale.
- **Contexto:** dá aulas em três turnos, reserva pelo celular entre uma aula e outra; precisa filtrar por capacidade e localização.
- **Frustração:** chegar na sala reservada e encontrar outra turma; descobrir só na hora que o laboratório estava em manutenção.

### P2 — Rodrigo, coordenador de laboratórios (perfil **Responsável**)
- **Objetivo:** aprovar ou rejeitar pedidos de recursos restritos com critério e acompanhar a retirada e a devolução de equipamentos.
- **Contexto:** recebe muitos pedidos na semana de matrícula; responde por equipamentos de alto valor.
- **Frustração:** equipamento retirado sem registro; não saber quem estava com o material quando ele voltou com defeito.

### P3 — Cláudia, secretaria acadêmica (perfil **Administrador**)
- **Objetivo:** manter o cadastro de salas, professores, materiais e usuários correto e bloquear recursos em manutenção.
- **Contexto:** responde à direção pelo uso dos espaços; precisa de relatório de utilização por período.
- **Frustração:** relatórios montados à mão; nenhuma forma de provar quantos conflitos o processo evitou.

## 4. Escopos **[Proposta]**
| ID | Escopo | Do que trata |
|---|---|---|
| E1 | Identidade e acesso | Login, perfis, regras de posse |
| E2 | Recursos | Cadastro e pesquisa de salas, professores e materiais |
| E3 | Reservas e agenda | Criar, alterar, cancelar; sobreposição; dupla reserva |
| E4 | Aprovação, manutenção e materiais | Aprovação de restritos, bloqueio por manutenção, retirada e devolução |
| E5 | Auditoria, relatórios e notificações | Histórico, relatório de utilização, notificação |

## 5. Fora de escopo **[Proposta]**
- Integração com o sistema acadêmico real da instituição e login institucional (SSO).
- Envio real de e-mail ou SMS (a notificação usa um provedor HTTP simulado — ver RF-11).
- Reservas recorrentes (série semanal) e lista de espera.
- Aplicativo móvel nativo (a interface web é responsiva).
- Cobrança, pagamento ou multa por atraso.

## 6. Requisitos funcionais
Os 14 requisitos obrigatórios da especificação **[Espec.]**, numerados pelo grupo na ordem da especificação. Critérios de aceite **[Proposta]**, derivados das regras e das aulas práticas.

| ID | Requisito [Espec.] | Escopo | Perfis | Critérios de aceite | Regras |
|---|---|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil | E1 | todos | Login devolve token; sem token → 401; perfil sem permissão → 403; cada operação segue a matriz de autorização | RN-10, RN-12 |
| RF-02 | Cadastro e consulta de salas, professores e materiais | E2 | Administrador (cadastro); todos autenticados (consulta) | Criar/editar só Administrador; dados inválidos (ex.: capacidade negativa) → 400 com mensagem compreensível | RN-12 |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | E2 | todos autenticados | Os 5 filtros combináveis; "disponível" exclui recursos com reserva ativa ou manutenção no intervalo | RN-02, RN-04 |
| RF-04 | Criação, alteração e cancelamento de reservas | E3 | Solicitante (próprias) | Solicitante só altera/cancela as próprias; cancelar muda o estado (nunca apaga) | RN-01, RN-08, RN-10 |
| RF-05 | Detecção de sobreposição de horários, inclusive na agenda do professor | E3 | — | Pedido que cruza reserva ativa do mesmo recurso → 409; vale para sala, material e professor | RN-02 |
| RF-06 | Proteção contra dupla reserva em solicitações simultâneas | E3 | — | 20 pedidos simultâneos ao mesmo recurso e horário → 1 aceito, 19 com 409, 1 linha no banco | RN-03 |
| RF-07 | Aprovação obrigatória para recursos restritos | E4 | Responsável | Reserva de restrito fica SOLICITADA até um Responsável aprovar; Solicitante não aprova | RN-05, RN-06 |
| RF-08 | Bloqueio de recursos em manutenção | E4 | Administrador | Pedido que toca período de manutenção → recusado; fronteiras testadas ao segundo | RN-04 |
| RF-09 | Registro de retirada e devolução de materiais e equipamentos | E4 | Responsável | Retirada leva a reserva a EM_USO; devolução a CONCLUÍDA; ambas auditadas | RN-07, RN-09 |
| RF-10 | Histórico auditável de mudanças | E5 | Responsável, Administrador (consulta) | Toda mudança de estado gera registro imutável na mesma transação; Solicitante → 403 na consulta | RN-09 |
| RF-11 | Notificação simulada ou integração com API externa | E5 | — | Mudanças de estado notificam via provedor HTTP externo; falha do provedor não desfaz a reserva | RN-13 |
| RF-12 | Relatório de utilização por recurso, carga horária alocada e conflitos evitados | E5 | Responsável, Administrador | `GET /relatorios/utilizacao?inicio=&fim=`; canceladas não contam; valores conferidos à mão nos testes | RN-11 |
| RF-13 | Interface responsiva e mensagens de erro compreensíveis | todos | todos | Telas utilizáveis de 360 px a 1920 px; erro em português, sem detalhe interno | RN-12 |
| RF-14 | Documentação de API ou dos fluxos públicos do sistema | todos | — | OpenAPI publicada e fluxos descritos no README | — |

## 7. Requisitos não funcionais
Classificados pelas características da **ISO/IEC 25010:2023**. Todo RNF tem **número** — sem número não é verificável.
As metas oficiais da disciplina estão marcadas **[Espec.]**; as demais são **[Proposta]** do grupo e serão validadas por medição.

| ID | Característica (ISO 25010) | Requisito | Medida | Origem | Como é verificado |
|---|---|---|---|---|---|
| RNF-01 | Eficiência de desempenho | Consulta de disponibilidade rápida sob carga | p95 ≤ 300 ms com 50 usuários simultâneos; erro < 1% | [Proposta] — exemplo da Aula 10 | JMeter, rodada registrada em `docs/performance/` |
| RNF-02 | Eficiência de desempenho | Criação de reserva rápida sob disputa | p95 ≤ 300 ms com 50 usuários; só 201 ou 409, **nunca 500** | [Proposta] — Aula 10 | JMeter com asserções |
| RNF-03 | Confiabilidade (integridade) | Nenhuma dupla reserva sob concorrência | 20 pedidos simultâneos → exatamente 1 aceito, em 5 execuções seguidas | [Espec.] regra crítica; números da Aula 10 | Teste de integração com Testcontainers |
| RNF-04 | Segurança (autorização) | Nenhum acesso indevido | 100% das células da matriz perfil × operação testadas; 0 divergências | [Espec.] limitador de nota | Teste parametrizado da matriz |
| RNF-05 | Segurança (confidencialidade) | Senhas e tokens protegidos | Senha só com hash BCrypt; token expira em 60 min; 0 segredos no repositório | [Proposta] | Teste + análise do SonarCloud |
| RNF-06 | Segurança (não vazamento) | Erro neutro | 0 respostas de erro com stack trace, SQL ou nome de classe interna | [Espec.] "tratamento seguro de erros"; Aula 12 | Testes de API sobre o corpo do erro |
| RNF-07 | Confiabilidade (tolerância a falhas) | Falha do provedor de notificação não derruba a reserva | Timeout ≤ 2 s; reserva confirmada mesmo com provedor em 500 ou lento | [Proposta] — Aula 08 | WireMock INT-01..INT-06 |
| RNF-08 | Segurança (responsabilização) | Trilha de auditoria completa | 100% das transições com registro; 0 registros órfãos após rollback | [Espec.] "toda mudança de estado deve gerar auditoria" | Teste de integração com rollback |
| RNF-09 | Capacidade de interação | Interface responsiva e mensagens compreensíveis | Telas utilizáveis de 360 px a 1920 px; mensagens em português | [Espec.] RF-13 | Teste end-to-end + revisão |
| RNF-10 | Manutenibilidade | Código coberto e analisado | Cobertura ≥ **80% de linhas** e ≥ **70% de branches**; **0 bugs ou vulnerabilidades críticas** | **[Espec.] metas mensuráveis** | JaCoCo no `verify` + Quality Gate do SonarCloud |
| RNF-11 | Manutenibilidade (rastreabilidade) | Requisitos críticos rastreados | **100% dos requisitos críticos na RTM** com teste e evidência do CI | **[Espec.] meta mensurável** | `docs/rtm.md` + mutação manual |
| RNF-12 | Flexibilidade (instalabilidade) | Qualquer pessoa sobe o sistema pelo README | Máquina limpa com JDK 21 + Docker: subir com 1 comando | [Espec.] limitador "aplicação que não executa" | Clone limpo na Aula 14 |
| RNF-13 | Manutenibilidade (testabilidade) | Suíte determinística | 5 execuções seguidas sem falha intermitente; tempo injetado por `Clock` | [Proposta] — Aulas 07 e 10 | CI + execuções repetidas |
| RNF-14 | Segurança (privacidade / LGPD) | Dados de teste sintéticos e logs limpos | 0 dados pessoais reais em testes; 0 tokens ou senhas em log | [Proposta] — Aulas 05 e 08 | Revisão + teste do log na integração |

## 8. Regras de negócio
As regras críticas da especificação **[Espec.]** e as que decorrem delas.

| ID | Regra | Origem | Crítica? |
|---|---|---|---|
| RN-01 | O término deve ser posterior ao início | [Espec.] | sim |
| RN-02 | Reservas ativas do mesmo recurso (sala, material ou professor) não podem se sobrepor | [Espec.] | sim |
| RN-03 | Duas solicitações simultâneas para o mesmo recurso e horário produzem uma única reserva aceita | [Espec.] | sim |
| RN-04 | Recursos em manutenção não podem ser reservados no período da manutenção | [Espec.] | sim |
| RN-05 | Somente o Responsável aprova reserva de recurso restrito | [Espec.] | sim |
| RN-06 | O Solicitante não aprova a própria reserva | Aula 09 | sim |
| RN-07 | A reserva só muda de estado pelas transições permitidas (seção 8.1) | [Espec.] | sim |
| RN-08 | Reserva iniciada (EM_USO, CONCLUÍDA, NÃO_COMPARECEU) não pode ser apagada | [Espec.] | sim |
| RN-09 | Toda mudança de estado gera registro de auditoria imutável, na mesma transação | [Espec.] | sim |
| RN-10 | O Solicitante só altera e cancela as próprias reservas | Aula 12 | sim |
| RN-11 | Reservas canceladas e rejeitadas não contam como uso no relatório | Aula 11 | não |
| RN-12 | Entrada inválida é recusada com 400 e mensagem compreensível, sem detalhe interno | [Espec.] + Aula 12 | não |
| RN-13 | A falha do provedor de notificação não desfaz nem bloqueia a mudança de estado | [Proposta] | não |

### 8.1 Máquina de estados da reserva
Estados **[Espec.]**: `SOLICITADA → APROVADA → EM_USO → CONCLUIDA`; alternativos `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU`.

| De | Para | Quem | Observação |
|---|---|---|---|
| — | SOLICITADA | Solicitante | Toda reserva nasce SOLICITADA |
| SOLICITADA | APROVADA | Responsável (recurso restrito) · sistema (recurso não restrito) | [Proposta] D-08: não restrito é aprovado automaticamente na criação |
| SOLICITADA | REJEITADA | Responsável | Libera o recurso |
| SOLICITADA | CANCELADA | Solicitante dono | Libera o recurso |
| APROVADA | CANCELADA | Solicitante dono | Libera o recurso |
| APROVADA | EM_USO | Responsável (retirada / início) | RF-09 |
| APROVADA | NAO_COMPARECEU | Responsável | Libera o recurso para o futuro; não apaga |
| EM_USO | CONCLUIDA | Responsável (devolução / fim) | RF-09 |
| CONCLUIDA, CANCELADA, REJEITADA, NAO_COMPARECEU | qualquer | — | **Proibido**: estados finais |

Estados que **ocupam** o recurso (entram na regra de sobreposição): SOLICITADA, APROVADA, EM_USO.

## 9. Decisões e perguntas em aberto
### 9.1 Decisões tomadas
Aceitas por Guilherme Viana Lopes em 2026-10-31. As decisões de arquitetura têm ADR em [`docs/adr/`](adr/).

| ID | Decisão | Registro |
|---|---|---|
| D-01 | `br.senac` / `organizacao-recursos` | `pom.xml` |
| D-04 | Autenticação por token JWT | ADR-003 |
| D-05 | Intervalos semiabertos `[início, término)`: reservas encostadas não conflitam | ADR-004 |
| D-06 | Dupla reserva impedida por exclusion constraint no PostgreSQL, traduzida em 409 | ADR-002 |
| D-07 | Uma reserva ocupa um único recurso | este PRD |
| D-08 | Recurso não restrito é aprovado automaticamente; restrito aguarda Responsável | seção 8.1 |
| D-09 | Só o Responsável aprova restrito (o Administrador não) | RN-05 |
| D-11 | "Reserva iniciada" é definida pelo estado, não pelo relógio | RN-08 |
| D-12 | Retirada e devolução movem a reserva para EM_USO e CONCLUIDA | seção 8.1 |
| D-13 | Notificação por porta + adaptador HTTP, testada com WireMock | ADR-005 |
| D-14 | No relatório, reserva que cruza a borda do período conta só as horas dentro dele | RF-12 |
| D-15 | RTM em `docs/rtm.md`, com `RTM.md` na raiz apontando para ela | RTM |
| D-16 | Migrações com Flyway | `pom.xml` |
| — | Spring Boot 3.5.16 | ADR-001 |

### 9.2 Perguntas em aberto
| ID | Pergunta | Situação |
|---|---|---|
| Q-01 | Tecnologia da interface (Thymeleaf + Tailwind, React no Maven ou React separado) e a ferramenta de teste end-to-end (D-02, D-03) | aguardando decisão do grupo |
| Q-02 | Manutenção cadastrada sobre reservas já existentes: bloquear o cadastro, cancelar as reservas ou só avisar? (D-10) | aguardando decisão; até lá, o cadastro é **recusado** se houver reserva ativa no período |
| Q-03 | Prazo mínimo para o Solicitante cancelar uma reserva aprovada | sem prazo até decisão |
| Q-04 | Tolerância para marcar NAO_COMPARECEU | marcação manual pelo Responsável |
| Q-05 | A restrição do cron diário de treino de ML (Aula 04) vale para este projeto? | entendida como restrição do case Foot Fanatics; confirmar com o professor |

## 10. Métricas de sucesso
| Métrica | Alvo | Origem |
|---|---|---|
| Duplas reservas aceitas | 0 | [Espec.] |
| Falhas de autorização | 0 | [Espec.] |
| Cobertura de linhas / branches | ≥ 80% / ≥ 70% | [Espec.] |
| Requisitos críticos na RTM com evidência | 100% | [Espec.] |
| Bugs ou vulnerabilidades críticas | 0 | [Espec.] |
| Conflitos evitados registrados no relatório | todos os 409 por sobreposição | RF-12 |
