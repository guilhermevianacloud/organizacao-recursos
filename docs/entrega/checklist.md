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
| Repositório com revisão por pares | sim | parcial — PRs com descrição, integrados na `main`; sem aprovação registrada de outro integrante | GitHub, PRs #1–#8 |
| Aplicação executável | sim | sim | `README.md` (como subir) |
| README e documentação da API | sim | sim | `README.md`; `/swagger-ui.html` |
| RTM | sim | sim | `docs/rtm.md` (e `RTM.md` na raiz) |
| Diagramas de contexto, componentes e sequências críticas | sim | sim | `docs/arquitetura.md` (Mermaid) |
| Plano e relatório de testes | sim | sim | `docs/plano-de-teste.md`; `docs/relatorio-qualidade.md`; artifacts do CI |
| Pipeline de CI | sim | sim | `.github/workflows/ci.yml` |
| Relatório do JaCoCo | sim | sim | artifact `relatorio-de-cobertura`; `docs/testes/lacunas-de-cobertura.md` |
| Relatório do SonarCloud | sim | sim | <https://sonarcloud.io/project/overview?id=guilhermevianacloud_organizacao-recursos>; passo no CI; DEF-004 |
| Plano e resultado do teste de carga | sim | sim | `perf/reservas.jmx`; `docs/performance/resultado-01.md` |
| Registro de defeitos | sim | sim | `docs/defeitos/` |
| Relatório final de qualidade | sim | sim | `docs/relatorio-qualidade.md` |

## Bloqueadores, em ordem
1. ~~A `main` só tem o primeiro commit.~~ **Resolvido em 2026-11-01:** PRs #1 a #8 integrados por Guilherme com merge
   commit; a `main` (`ba66400`) é idêntica à branch da última fase e o CI passou nela (execução 41341089727).
2. ~~Tag `v1.0.0-rc1`.~~ **Resolvido em 2026-11-01:** tag anotada no commit `ba66400`. Substituída como versão
   avaliada pela `v1.0.0-rc2`, criada em 2026-11-02 no commit `8be1deb` com CI verde (execução 41377043205), que inclui
   as correções do SonarCloud (DEF-004), o ciclo ATAM 01 e a arquitetura atualizada.
3. **Decisão de qualidade:** responsáveis pelos riscos aceitos e a declaração de cada integrante ainda em branco (`docs/decisao-qualidade.md`).
4. ~~SonarCloud.~~ **Configurado em 2026-11-01:** Quality Gate aprovado no PR #10. Falta o Guilherme revisar o S4502 no
   site, se concordar com o ADR-007.
5. **Revisão por pares:** PRs sem aprovação de outro integrante; se Rafael ou Gustavo revisarem, a evidência fica mais forte.
