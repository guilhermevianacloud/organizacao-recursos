# Organização de Recursos

Sistema para alocar **salas, professores e materiais sem conflitos de horário**.
Projeto final da disciplina **Qualidade de Software 2026.2** (Senac, prof. Afonso Lelis) — turma STADSCAS4NB.

Integrantes: Guilherme Viana Lopes · Rafael Souza · Gustavo Santana.

## Stack
- Java 21 (Eclipse Temurin)
- Spring Boot 3.5.16 (ver [ADR-001](docs/adr/ADR-001-versao-spring-boot.md))
- PostgreSQL 17 (em container) · Flyway
- Maven Wrapper (`./mvnw`) — não é preciso instalar Maven

## Pré-requisitos
- **JDK 21**
- **Docker** em execução (a aplicação e os testes usam PostgreSQL em container)

> **Windows:** clone numa pasta de caminho curto (ex.: `C:\projetos\`) ou habilite caminhos longos antes de clonar:
> `git config --global core.longpaths true`. Em pastas muito profundas, o limite de 260 caracteres do Windows
> impede o Git de criar alguns arquivos ("Filename too long") — encontrado no teste de clone limpo.

## Como rodar os testes
```bash
./mvnw -B verify
```
No Windows (PowerShell): `.\mvnw.cmd -B verify`

O `verify` compila, roda todos os testes contra um PostgreSQL real (Testcontainers) e gera o pacote.

## Como subir a aplicação
```bash
./mvnw spring-boot:run
```
O Spring Boot sobe sozinho o PostgreSQL descrito em `compose.yaml` e aplica as migrações do Flyway.

Depois abra **http://localhost:8080** no navegador.

### Usuários de demonstração
Criados automaticamente quando o banco está vazio (dados fictícios, domínio `.invalid`). Senha de demonstração: `demo123`
(troque com a variável `DEMO_SENHA`; desligue com `app.demo.habilitado=false`).

| Perfil | E-mail | O que pode fazer |
|---|---|---|
| Solicitante | `solicitante@demo.invalid` | pesquisar recursos livres, reservar, alterar e cancelar as próprias reservas |
| Responsável | `responsavel@demo.invalid` | aprovar ou rejeitar restritos, registrar retirada, devolução e não comparecimento, ver histórico e relatório |
| Administrador | `admin@demo.invalid` | cadastrar recursos, bloquear para manutenção, ver histórico e relatório |

### Variáveis de ambiente (opcionais)
| Variável | Para quê | Sem ela |
|---|---|---|
| `JWT_SEGREDO` | chave de assinatura dos tokens (mínimo 32 caracteres) | chave aleatória a cada início (login precisa ser refeito ao reiniciar) |
| `DEMO_SENHA` | senha dos usuários de demonstração | `demo123` |
| `NOTIFICACAO_URL`, `NOTIFICACAO_CHAVE` | provedor HTTP de notificação | notificação simulada (registrada no log) |

## API
Documentação interativa (OpenAPI): **http://localhost:8080/swagger-ui.html**. Toda rota de `/api` exige o token
obtido em `POST /api/auth/login`, enviado como `Authorization: Bearer <token>`. Quem pode chamar cada rota está em
[docs/seguranca/matriz-autorizacao.md](docs/seguranca/matriz-autorizacao.md). Erros seguem o formato ProblemDetail,
em português, com um `codigo` estável (ex.: `CONFLITO_DE_HORARIO`).

## Documentação
| Documento | Conteúdo |
|---|---|
| [docs/prd.md](docs/prd.md) | Produto, personas, 14 requisitos funcionais, 14 não funcionais, regras e estados |
| [docs/arquitetura.md](docs/arquitetura.md) | C4, modelo de dados, fluxos críticos, utility tree, cenários e análise ATAM |
| [docs/avaliacao-arquitetural/ciclo-01.md](docs/avaliacao-arquitetural/ciclo-01.md) | Ciclo ATAM sobre o código pronto: cenários medidos, táticas com arquivo e linha, divergências e ações |
| [docs/plano-de-teste.md](docs/plano-de-teste.md) | Estratégia e plano de testes (ISO/IEC/IEEE 29119-3) |
| [docs/testes/plano-tdd.md](docs/testes/plano-tdd.md) | Inventário de casos de teste por regra de negócio |
| [docs/rtm.md](docs/rtm.md) | Matriz de rastreabilidade requisito → risco → teste → evidência |
| [docs/status-requisitos.md](docs/status-requisitos.md) | Situação de cada requisito e limitador de nota, com o teste que prova |
| [docs/seguranca/matriz-autorizacao.md](docs/seguranca/matriz-autorizacao.md) | Perfil × operação; lida pelo teste de autorização |
| [docs/testes/evidencia-tdd.md](docs/testes/evidencia-tdd.md) | Commits vermelho e verde de cada ciclo de TDD |
| [docs/testes/bdd-aprovacao-bloqueio.md](docs/testes/bdd-aprovacao-bloqueio.md) | Cenários BDD e tabela de fronteiras |
| [docs/adr/](docs/adr/) | Registros de decisão arquitetural |
| [docs/defeitos/](docs/defeitos/) | Registro de defeitos (cada um com issue no GitHub, commits e teste de regressão) |
| [docs/taticas-arquiteturais-len-bass.md](docs/taticas-arquiteturais-len-bass.md) | Catálogo de táticas (material de apoio da Aula 04) |

## Integração contínua
O workflow [`qualidade`](.github/workflows/ci.yml) roda `./mvnw -B verify` a cada pull request e a cada push na `main`. Os relatórios de teste ficam como artifact de cada execução, inclusive quando ela falha.

Análise estática: [SonarCloud](https://sonarcloud.io/project/overview?id=guilhermevianacloud_organizacao-recursos), no mesmo workflow, lendo a cobertura do JaCoCo. O passo só roda quando o secret `SONAR_TOKEN` existe (forks e clones sem o secret não falham por isso).

Evidência de que o CI barra falhas (Aula 06), registrada no PR #1:

| Execução | Commit | Resultado | O que aconteceu |
|---|---|---|---|
| [41207827313](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41207827313) | `8ea556d` | verde | Primeira execução do pipeline |
| [41207912465](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41207912465) | `5b93d2e` | **vermelho** | Quebra intencional: o teste passou a esperar PostgreSQL 16 e falhou por asserção |
| [41207988750](https://github.com/guilhermevianacloud/organizacao-recursos/actions/runs/41207988750) | `9373d64` | verde | Quebra revertida com `git revert`; suíte de volta ao verde |

## Uso de IA
O código deste repositório é escrito com apoio de agente de IA (Claude), seguindo os prompts das aulas práticas da disciplina. Toda mudança passa por pull request, CI e revisão do grupo.
