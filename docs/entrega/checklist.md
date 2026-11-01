# Checklist da entrega em clone limpo

Aula 14 (Prompt 1). O repositório foi clonado num diretório novo, **sem reaproveitar nada da máquina de quem
desenvolveu** — inclusive com um repositório Maven vazio (`-Dmaven.repo.local` apontando para uma pasta nova) — e
seguiu-se **apenas o README**. Data: 2026-11-01. Branch clonada: `fase-8/relatorio-prova-oral` (commit `c645712`),
porque a `main` ainda não recebeu os pull requests (ver bloqueador 1).

## Passos do README
| Passo | Resultado |
|---|---|
| `git clone …` numa pasta de caminho longo (pasta temporária do sistema) | **falhou** no Windows: "Filename too long" em 7 arquivos (limite de 260 caracteres). Corrigido com aviso no README (`core.longpaths` ou caminho curto). |
| `git clone …` em `C:\Users\…\cl` | ok |
| `./mvnw -B verify` com repositório Maven vazio | 1ª tentativa: queda de rede ao baixar do Maven Central ("Connection reset") — não é defeito do projeto; 2ª tentativa (reaproveitando o que já tinha baixado nessa mesma pasta nova): **BUILD SUCCESS** — unidade **98/98**, integração **175/175**, "All coverage checks have been met"; 353 MB de dependências baixadas do zero |
| Aplicação sobe e responde | provado dentro do mesmo `verify` por `InterfaceE2EIT` (sobe a aplicação inteira e usa um navegador real); `./mvnw spring-boot:run` executado com sucesso pelo Guilherme no mesmo dia (4 migrações, dados de demonstração, porta 8080) |

## Entregáveis da especificação
| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório com revisão por pares | sim | parcial — PRs abertos com descrição; aprovação de outro integrante pendente | GitHub, PRs #1–#8 |
| Aplicação executável | sim | sim | `README.md` (como subir) |
| README e documentação da API | sim | sim | `README.md`; `/swagger-ui.html` |
| RTM | sim | sim | `docs/rtm.md` (e `RTM.md` na raiz) |
| Diagramas de contexto, componentes e sequências críticas | sim | sim | `docs/arquitetura.md` (Mermaid) |
| Plano e relatório de testes | sim | sim | `docs/plano-de-teste.md`; `docs/relatorio-qualidade.md`; artifacts do CI |
| Pipeline de CI | sim | sim | `.github/workflows/ci.yml` |
| Relatório do JaCoCo | sim | sim | artifact `relatorio-de-cobertura`; `docs/testes/lacunas-de-cobertura.md` |
| Relatório do SonarCloud | **não** | — | aguardando conta, projeto e `SONAR_TOKEN` |
| Plano e resultado do teste de carga | sim | sim | `perf/reservas.jmx`; `docs/performance/resultado-01.md` |
| Registro de defeitos | sim | sim | `docs/defeitos/` |
| Relatório final de qualidade | sim | sim | `docs/relatorio-qualidade.md` |

## Bloqueadores, em ordem
1. **A `main` só tem o primeiro commit.** O professor clona a `main` e segue o README: sem os merges dos PRs #1 a #8
   (nessa ordem, com merge commit), ele não encontra o sistema — cai no limitador "aplicação que não executa".
   **Responsável: Guilherme (merge no GitHub).**
2. **Tag `v1.0.0-rc1`** só pode ser criada depois do item 1, no commit da `main` com CI verde.
3. **Decisão de qualidade:** responsáveis pelos riscos aceitos e a declaração de cada integrante ainda em branco (`docs/decisao-qualidade.md`).
4. **SonarCloud** (não bloqueia a execução, mas deixa "0 bugs ou vulnerabilidades críticas" como não medido).
5. **Revisão por pares:** PRs sem aprovação de outro integrante; se Rafael ou Gustavo revisarem, a evidência fica mais forte.
