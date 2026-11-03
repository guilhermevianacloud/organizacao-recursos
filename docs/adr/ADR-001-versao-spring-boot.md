# ADR-001 — Versão do Spring Boot

Status: aceita (2026-10-31, Guilherme Viana Lopes)

## Contexto e forças
- A especificação do projeto define a stack como **"Java 21 · Spring Boot 3.x"**.
- Em 2026-10-31 o start.spring.io oferece apenas Spring Boot 4.x (padrão 4.1.2; mais antiga 4.0.9).
- No Maven Central, a última versão 3.x publicada é a **3.5.16**.
- Segundo spring.io, a linha 3.5.x teve suporte open source até **2026-06-30** (suporte comercial até 2032-06-30); a 4.1.x tem suporte open source até 2027-07-31.

## Requisitos e cenários
- Especificação: "Java 21 · Spring Boot 3.x".
- RNF-12 (instalabilidade) e CEN-01 a CEN-06 dependem de uma versão estável e com documentação, que o professor e o grupo consigam rodar.

## Alternativas
- (a) Spring Boot 3.5.16 — cumpre a especificação ao pé da letra; coincide com o material das aulas.
- (b) Spring Boot 4.1.2 — atual e suportado, mas diverge do texto da especificação e muda APIs em relação ao que foi visto em aula.
- (c) Perguntar ao professor antes — atrasa o início do código.

## Decisão
(a) **Spring Boot 3.5.16**, montado sem o Initializr (que não gera mais 3.x); o Maven Wrapper foi copiado do Initializr.

## Consequências
- \+ Atende a especificação ("3.x") e o que foi ensinado.
- − A linha 3.5 não recebe mais correções open source; eventuais vulnerabilidades novas não terão patch público.

## Riscos
- Dependência com vulnerabilidade conhecida apontada pelo SonarCloud sem versão corrigida disponível na linha 3.5.

## Evidência necessária
- `./mvnw -B verify` verde com Spring Boot 3.5.16 (log do CI).

## Condição de revisão
- Se o professor aceitar 4.x, ou se surgir vulnerabilidade crítica sem correção na 3.5, avaliar migração para 4.x em novo ADR.
