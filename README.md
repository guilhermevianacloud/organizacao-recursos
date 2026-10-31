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

## Documentação
| Documento | Conteúdo |
|---|---|
| [docs/prd.md](docs/prd.md) | Produto, personas, requisitos e regras |
| [docs/arquitetura.md](docs/arquitetura.md) | Arquitetura, decisões e análise ATAM |
| [docs/plano-de-teste.md](docs/plano-de-teste.md) | Estratégia e plano de testes |
| [docs/adr/](docs/adr/) | Registros de decisão arquitetural |

## Uso de IA
O código deste repositório é escrito com apoio de agente de IA (Claude), seguindo os prompts das aulas práticas da disciplina. Toda mudança passa por pull request, CI e revisão do grupo.
