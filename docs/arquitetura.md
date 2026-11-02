# Organização de Recursos — Arquitetura

> Fontes: [`docs/prd.md`](prd.md) (requisitos e regras), [Especificação do Projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html), catálogo de táticas em [`docs/taticas-arquiteturais-len-bass.md`](taticas-arquiteturais-len-bass.md) (material da Aula 04).
> Convenção: **Fato** (está nas fontes) · **Suposição** (S-xx, precisa ser confirmada) · **Decisão** (D-xx/ADR). Métricas sem medição ainda são marcadas **validação pendente**.
> Estado em 2026-11-01: arquitetura **como construída** (`main` em `16adf58`). A versão proposta em 2026-10-31 foi comparada com o código no [ciclo ATAM 01](avaliacao-arquitetural/ciclo-01.md); as divergências encontradas lá (seção 6) foram corrigidas aqui. O histórico do Git guarda a versão proposta.

## 1. Stack
| Item | Escolha | Origem |
|---|---|---|
| Linguagem | Java 21 (Temurin) | Especificação |
| Framework | Spring Boot 3.5.16 | Especificação ("3.x") + [ADR-001](adr/ADR-001-versao-spring-boot.md) |
| Banco | PostgreSQL 17 | Aulas 03, 08, 10 (PostgreSQL real, sem H2) |
| Migrações | Flyway | D-16 |
| Segurança | Spring Security + JWT | RF-01 + [ADR-003](adr/ADR-003-autenticacao-jwt.md) |
| Build | Maven Wrapper | Aula 03 |
| Interface | HTML + CSS + JavaScript servidos pelo próprio Spring | [ADR-006](adr/ADR-006-interface-web.md) |
| Testes | JUnit 5, Mockito (só unidade), Testcontainers, WireMock, Cucumber, Playwright, JaCoCo, SonarCloud, JMeter | Especificação + `docs/plano-de-teste.md` |

## 2. Drivers de negócio
| ID | Driver | Origem |
|---|---|---|
| DR-01 | Nenhum recurso pode ser alocado duas vezes no mesmo horário | Especificação: objetivo + regras críticas |
| DR-02 | Só quem tem o perfil certo executa cada operação | Especificação: RF-01, limitador "falha de autorização" |
| DR-03 | Toda mudança precisa ser rastreável a quem e quando | Especificação: RF-10, regra de auditoria |
| DR-04 | Qualquer avaliador sobe e testa o sistema seguindo só o README | Especificação: limitador "aplicação que não executa"; Aula 14 |
| DR-05 | A consulta de disponibilidade precisa responder rápido no pico (semana de matrícula) | Catálogo Len Bass, seção 8; Aula 10 |

## 3. Visão de contexto (C4 — nível 1)
```mermaid
flowchart LR
    sol["Solicitante<br/>(professor ou coordenador)"]
    resp["Responsável<br/>(aprova restritos, materiais)"]
    adm["Administrador<br/>(cadastros, manutenção)"]
    sis["Organização de Recursos<br/>[sistema]<br/>Reserva salas, professores e materiais sem conflito"]
    notif["Provedor de notificação<br/>[sistema externo, simulado]"]
    sol -->|"consulta disponibilidade, reserva"| sis
    resp -->|"aprova, registra retirada/devolução"| sis
    adm -->|"cadastra recursos, bloqueia manutenção"| sis
    sis -->|"envia avisos de mudança de estado (HTTP)"| notif
```

## 4. Contêineres (C4 — nível 2)
```mermaid
flowchart LR
    usuario["Usuários<br/>(navegador)"]
    subgraph sistema["Organização de Recursos"]
        app["Aplicação Spring Boot<br/>[Java 21]<br/>API REST + interface web"]
        db[("PostgreSQL 17<br/>[banco]<br/>reservas, recursos, auditoria")]
    end
    notif["Provedor de notificação<br/>[HTTP externo]"]
    usuario -->|"HTTPS + JWT"| app
    app -->|"JDBC; exclusion constraint impede sobreposição"| db
    app -->|"HTTP com timeout"| notif
```
- Um único contêiner de aplicação: a interface web é servida pela própria aplicação (ADR-006).
- O banco é o **ponto único de verdade** para conflito de horário: todas as instâncias da aplicação passam por ele (ADR-002).

## 5. Componentes da aplicação (C4 — nível 3)
```mermaid
flowchart TB
    subgraph web["api / seguranca"]
        sec["Spring Security + JWT<br/>(ConfiguracaoDeSeguranca, ServicoDeToken)"]
        ctrl["Controllers REST<br/>(Autenticacao, Recurso, Reserva, Manutencao, Relatorio)<br/>validação de entrada, @PreAuthorize"]
        erros["TratamentoDeErros<br/>(ProblemDetail com código)"]
    end
    subgraph servico["servico"]
        res["ServicoDeReservas<br/>(solicitar, alterar, cancelar, aprovar, rejeitar,<br/>retirada, devolução, não comparecimento, histórico)"]
        rec["ServicoDeRecursos<br/>(cadastro, pesquisa)"]
        man["ServicoDeManutencao"]
        rel["ServicoDeRelatorios"]
        conf["RegistroDeConflitos<br/>(transação própria)"]
    end
    subgraph dominio["dominio"]
        intervalo["Intervalo<br/>(semiaberto, sobreposição)"]
        estados["EstadoReserva<br/>(transições permitidas)"]
        reserva["Reserva, Recurso, Manutencao,<br/>AuditoriaReserva (imutável)"]
    end
    subgraph infra["persistencia / notificacao"]
        repo["Repositórios JPA<br/>(trava por recurso)"]
        porta["Notificador<br/>(porta)"]
        adapt["NotificadorHttp / NotificadorSimulado"]
    end
    sec --> ctrl
    ctrl --> res & rec & man & rel
    ctrl -->|"409: registra a recusa"| conf
    ctrl -.-> erros
    res --> intervalo & estados & reserva & repo
    res -->|"evento depois do commit"| porta
    man --> intervalo & repo
    rec --> repo
    rel --> repo
    porta --> adapt
```
- **Por que separar intervalo e estados no domínio:** são as regras críticas mais testadas; isoladas, viram testes de unidade rápidos e com valores-limite (`IntervaloTest`, `EstadoReservaTest`).
- **Por que um só serviço de reservas:** aprovação, retirada e devolução são transições da mesma reserva e precisam da mesma auditoria na mesma transação. A proposta inicial tinha serviços separados de aprovação e auditoria (ciclo 01, divergência 2).
- **Por que a porta do notificador:** só o adaptador conhece URL, token e formato do provedor; o domínio fala com uma interface. É o padrão do Foot Fanatics na Aula 08 (tática *Tailor Interface + Abstract Data Sources*).

## 6. Modelo de dados (migrações V2 a V4)
```mermaid
erDiagram
    USUARIO ||--o{ RESERVA : solicita
    RECURSO ||--o{ RESERVA : "é reservado em"
    RECURSO ||--o{ MANUTENCAO : "fica em"
    RESERVA ||--o{ AUDITORIA_RESERVA : "gera"
    USUARIO {
        bigint id
        text nome
        text email
        text perfil "SOLICITANTE | RESPONSAVEL | ADMINISTRADOR"
        text senha_hash
    }
    RECURSO {
        bigint id
        text tipo "SALA | MATERIAL | PROFESSOR"
        text nome
        int capacidade
        text localizacao
        text competencias
        boolean restrito
        boolean ativo
    }
    RESERVA {
        bigint id
        bigint recurso_id
        bigint solicitante_id
        timestamptz inicio
        timestamptz termino
        text estado
        text motivo
        bigint versao
    }
    MANUTENCAO {
        bigint id
        bigint recurso_id
        timestamptz inicio
        timestamptz termino
        text motivo
    }
    AUDITORIA_RESERVA {
        bigint id
        bigint reserva_id
        bigint usuario_id
        timestamptz instante
        text estado_anterior
        text estado_novo
        text motivo
    }
    TENTATIVA_RECUSADA {
        bigint id
        bigint recurso_id
        timestamptz inicio
        timestamptz termino
        timestamptz instante
    }
```
- **Recurso único com `tipo`** (D-07): sala, material e professor são "recursos reserváveis". Assim **uma única exclusion constraint** (`reserva_sem_sobreposicao`, V3) protege os três tipos contra sobreposição, e só para os estados que ocupam: SOLICITADA, APROVADA e EM_USO (ADR-002).
- **Professor como recurso:** a agenda do professor entra na mesma regra de sobreposição (RF-05 "inclusive na agenda do professor").
- **Retirada e devolução (RF-09)** são transições da reserva (APROVADA → EM_USO → CONCLUIDA), registradas na auditoria. Não há tabela separada de movimentação, que existia na proposta inicial.
- **Conflitos evitados (RF-12):** `tentativa_recusada` (V4), sem chave estrangeira, gravada em transação própria (`RegistroDeConflitos`) para sobreviver ao rollback do pedido recusado.
- Os textos ficam em `VARCHAR` com `CHECK` para perfil, tipo e estado; mudanças entram por migração Flyway.

## 7. Fluxos críticos (sequência)
### 7.1 Criar reserva com disputa pelo mesmo recurso
```mermaid
sequenceDiagram
    participant A as Solicitante A
    participant B as Solicitante B
    participant API as Aplicação
    participant DB as PostgreSQL
    A->>API: POST /api/reservas (sala 101, 08:00–10:00)
    B->>API: POST /api/reservas (sala 101, 09:00–11:00)
    API->>API: valida entrada (RN-01)
    API->>DB: SELECT recurso FOR UPDATE (pedido A trava a sala 101)
    API->>DB: SELECT recurso FOR UPDATE (pedido B espera)
    API->>DB: A: confere manutenção (RN-04) e sobreposição; INSERT reserva + auditoria; COMMIT
    DB-->>API: trava liberada; B prossegue
    API->>DB: B: confere sobreposição → conflito; ROLLBACK
    API->>DB: B: controller registra a tentativa recusada (transação própria)
    API-->>A: 201 Created
    API-->>B: 409 Conflict (ProblemDetail com código)
```
Duas camadas (ADR-002, atualizado pelo DEF-002): a **trava por recurso** faz os pedidos do mesmo recurso esperarem a vez, o que evita deadlock e dá uma mensagem clara; a **exclusion constraint** é a garantia final para qualquer caminho de escrita, inclusive SQL direto (`ProtecaoNoBancoIT`). Custo medido: fila no pico (DEF-003).

### 7.2 Aprovar recurso restrito
```mermaid
sequenceDiagram
    participant S as Solicitante
    participant R as Responsável
    participant API as Aplicação
    participant DB as PostgreSQL
    participant N as Provedor de notificação
    S->>API: POST /api/reservas (laboratório restrito)
    API->>DB: reserva SOLICITADA + auditoria
    API-->>S: 201 (SOLICITADA)
    R->>API: POST /api/reservas/{id}/aprovacao
    API->>API: perfil = Responsável? (RN-05) · não é o próprio solicitante? (RN-06) · transição permitida? (RN-07)
    API->>DB: estado APROVADA + auditoria (mesma transação)
    API-->>R: 200
    API->>N: depois do commit: notifica (timeout 2 s)
    N--xAPI: falha ou demora
    Note over API,N: A aprovação já foi gravada; a falha do provedor não a desfaz (RN-13, ADR-005)
```

## 8. Autenticação e sessão
- Login (`POST /api/auth/login`) devolve um **JWT** (HS256, 60 min) assinado com o perfil do usuário ([ADR-003](adr/ADR-003-autenticacao-jwt.md)).
- Cada requisição envia `Authorization: Bearer <token>`; o filtro do Spring Security valida a assinatura e a expiração. Sem sessão no servidor e sem cookie, por isso o CSRF fica desligado ([ADR-007](adr/ADR-007-csrf-desligado.md), proposta).
- Autorização em duas camadas: **por perfil** (`@PreAuthorize`, matriz em [`docs/seguranca/matriz-autorizacao.md`](seguranca/matriz-autorizacao.md)) e **por posse** (RN-10: o dono da reserva; quem pediu não aprova).
- Senhas com hash **BCrypt**; a chave de assinatura do JWT vem da variável `JWT_SEGREDO`, nunca do código (sem ela, uma chave aleatória por execução).

## 9. Atributos de qualidade e utility tree
Prioridade: (Importância, Dificuldade) em A/M/B. Analisar primeiro os (A, A).

| Atributo (ISO 25010) | Refinamento | Cenário | (I, D) |
|---|---|---|---|
| Confiabilidade | Integridade sob concorrência | CEN-01 — dupla reserva | (A, A) |
| Segurança | Autorização por perfil e posse | CEN-02 — acesso indevido | (A, M) |
| Segurança | Responsabilização | CEN-03 — auditoria com rollback | (A, M) |
| Eficiência de desempenho | Latência no pico | CEN-04 — consulta de disponibilidade | (M, M) |
| Confiabilidade | Tolerância a falha externa | CEN-05 — provedor de notificação fora | (M, B) |
| Manutenibilidade | Testabilidade do tempo | CEN-06 — regra de manutenção na fronteira | (M, B) |

## 10. Cenários de qualidade (seis partes)
| ID | Fonte | Estímulo | Ambiente | Artefato | Resposta | Medida |
|---|---|---|---|---|---|---|
| CEN-01 | 20 solicitantes | pedem a mesma sala no mesmo horário ao mesmo tempo | operação normal, pico | serviço de reservas + banco | exatamente um pedido aceito, os outros recusados com 409 | 1 aceito, 19 × 409, 1 linha no banco, em 5 de 5 execuções |
| CEN-02 | Solicitante autenticado | tenta cancelar a reserva de outro pelo id | operação normal | API de reservas | acesso negado sem revelar a reserva alheia | 100% das tentativas recusadas (403); 0 alterações |
| CEN-03 | Responsável | aprova uma reserva e a gravação falha no meio | falha de banco durante a transação | serviço de aprovação + auditoria | nada é gravado: nem a mudança, nem a auditoria | 0 registros de auditoria órfãos |
| CEN-04 | 50 usuários | consultam disponibilidade simultaneamente | pico, máquina local | API de pesquisa | resposta dentro do limite | p95 ≤ 300 ms, erro < 1% (proposta do grupo; **medido: 332 ms, não atende**, DEF-003) |
| CEN-05 | Provedor de notificação | responde 500 ou passa do timeout | operação normal | adaptador de notificação | reserva confirmada; falha registrada | reserva gravada em 100% dos casos; espera pelo provedor ≤ 2 s (**medido: atende**, `NotificadorHttpIT`) |

Resultado de cada cenário e a evidência: [ciclo ATAM 01](avaliacao-arquitetural/ciclo-01.md), seção 2.
| CEN-06 | Solicitante | pede reserva que termina exatamente no início da manutenção | operação normal | regra de manutenção | reserva aceita (intervalo semiaberto) | 3 casos de fronteira (−1 s, 0, +1 s) com resultado esperado |

## 11. Análise ATAM
Táticas pelo catálogo de Len Bass (seção 8 trata deste projeto). Roteiro por decisão: tática · cenário · métrica · custo · teste · risco se faltar.
Esta é a análise feita **antes** do código (2026-10-31). O [ciclo 01](avaliacao-arquitetural/ciclo-01.md) refez a análise com o código e as medições: táticas com arquivo e linha, trade-off integridade × latência confirmado, riscos novos R-04 a R-07.

| Decisão | Tática | Cenário | Custo / trade-off | Teste que evidencia |
|---|---|---|---|---|
| Exclusion constraint + trava por recurso (ADR-002) | Transactions + Schedule Resources | CEN-01 | os pedidos do mesmo recurso esperam a vez (**ponto de trade-off**: integridade × latência, medido no DEF-003) | concorrência 20 → 1 com Testcontainers |
| Matriz perfil × operação + posse (ADR-003) | Authenticate + Authorize Actors | CEN-02 | modelo granular aumenta atrito e complexidade da matriz | teste parametrizado lendo a matriz |
| Auditoria na mesma transação | Audit + Timestamp + Transactions | CEN-03 | volume de dados e dado pessoal (**trade-off: auditoria × LGPD**) | integração com rollback forçado |
| Cancelar muda estado, não apaga | Cancel + Undo | RN-08 | histórico ocupa armazenamento | transições proibidas parametrizadas |
| Porta + adaptador do notificador (ADR-005) | Tailor Interface + Timeout + Degradation | CEN-05 | indireção extra; dublê pode ficar desatualizado | WireMock INT-01..INT-06 |
| `Clock` injetado nas regras | Limit Nondeterminism (testabilidade) | CEN-06 | mais um parâmetro em cada serviço | trinca de limites com `Clock.fixed` |

**Descobertas**
- **Ponto de sensibilidade:** o operador de comparação da sobreposição (`<` × `<=`) decide sozinho se reservas encostadas conflitam (ADR-004).
- **Ponto de trade-off:** um cache de disponibilidade melhoraria CEN-04, mas poderia mostrar livre uma sala já reservada. **Decisão: sem cache** até a medição de desempenho mostrar necessidade.
- **Risco R-01:** se a constraint considerar reservas canceladas, recursos ficam bloqueados para sempre → a constraint vale só para estados que ocupam (SOLICITADA, APROVADA, EM_USO).
- **Risco R-02:** autorização só por perfil, sem posse, permitiria IDOR → posse verificada no serviço (RN-10).
- **Risco R-03:** notificação dentro da transação faria uma falha externa desfazer a reserva → notificar **depois** do commit (RN-13).
- **Não-risco:** queda do provedor de notificação não afeta reservas, porque o envio acontece fora da transação e com timeout.
- **Tema de risco:** todas as regras críticas dependem de tempo — por isso `Clock` injetado em todas.

## 12. Matriz requisito → decisão → componente → evidência
| Requisito | Decisão / ADR | Componente | Evidência de verificação |
|---|---|---|---|
Evidência de todas as linhas: [CI run 41348450251](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41348450251) (commit `16adf58`). A rastreabilidade completa, com risco e status, está em [`docs/rtm.md`](rtm.md).

| Requisito | Decisão / ADR | Componente | Evidência de verificação |
|---|---|---|---|
| RF-06 / RN-03 | ADR-002 (constraint + trava por recurso) | migração V3 + `ServicoDeReservas` + `RecursoRepository` | `DuplaReservaConcorrenciaIT`, `ProtecaoNoBancoIT` |
| RF-05 / RN-02 | ADR-002, ADR-004 | `Intervalo` + constraint | `IntervaloTest`, `ServicoDeReservasIT` |
| RF-01 / RN-10 | ADR-003, ADR-007 | Spring Security + controllers + posse no serviço | `MatrizDeAutorizacaoIT` (88 células), `ApiDeReservasIT` |
| RF-10 / RN-09 | auditoria na mesma transação | `ServicoDeReservas` + `AuditoriaReserva` (imutável) | `AuditoriaIT` (inclusive rollback) |
| RF-11 / RN-13 | ADR-005 | porta `Notificador` + `NotificadorHttp` | `NotificadorHttpIT` (INT-01..06), `NotificacaoAposCommitIT` |
| RF-12 | `tentativa_recusada` em transação própria | `ServicoDeRelatorios` + `RegistroDeConflitos` | `RelatorioDeUtilizacaoIT` |
| RF-13 | ADR-006 | interface estática + `TratamentoDeErros` | `InterfaceE2EIT`, `ApiDeReservasIT` |
| RNF-01/02 | sem cache (seção 11) | API de pesquisa e de reservas | `perf/reservas.jmx`, `docs/performance/resultado-01.md` — **não atende** (DEF-003) |
| RNF-12 | ADR-001, `compose.yaml` | build e README | CI + clone limpo (`docs/entrega/checklist.md`) |

## 13. Suposições, perguntas e riscos aceitos
- **S-01:** um único contêiner de aplicação basta para o volume de uma instituição de ensino (sem medição ainda).
- **S-02:** o provedor de notificação aceita uma chamada HTTP JSON simples; não há provedor real (RF-11 permite simulação).
- **Q-05 (PRD):** a restrição do cron diário de treino de ML, presente nos prompts da Aula 04, é do case Foot Fanatics; **não foi incorporada**. Confirmar com o professor.
- **Riscos aceitos (com responsável):** Spring Boot 3.5 fora do suporte gratuito (ADR-001, Guilherme Viana Lopes); token não revogável antes de expirar (ADR-003).
- **Riscos com aceite ainda proposto, aguardando o grupo:** DEF-003 (desempenho abaixo da meta proposta) e CSRF desligado (ADR-007). Ver `docs/decisao-qualidade.md`.

## 14. Decisões (ADR)
- [ADR-001 — Versão do Spring Boot](adr/ADR-001-versao-spring-boot.md)
- [ADR-002 — Proteção contra dupla reserva no banco](adr/ADR-002-protecao-dupla-reserva.md)
- [ADR-003 — Autenticação com JWT](adr/ADR-003-autenticacao-jwt.md)
- [ADR-004 — Intervalos semiabertos](adr/ADR-004-intervalos-semiabertos.md)
- [ADR-005 — Notificação por porta e adaptador HTTP](adr/ADR-005-notificacao-porta-adaptador.md)
- [ADR-006 — Interface web estática servida pelo Spring](adr/ADR-006-interface-web.md)
- [ADR-007 — Proteção CSRF desligada na API stateless](adr/ADR-007-csrf-desligado.md)
