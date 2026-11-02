# ADR-003 — Autenticação com JWT

Status: aceita (2026-10-31, Guilherme Viana Lopes) · decisão D-04

## Contexto e forças
- RF-01: autenticação e autorização por perfil (Solicitante, Responsável, Administrador).
- Os prompts das aulas usam "tokens de cada perfil" nos testes de API (Aulas 09 e 12), e o plano JMeter faz "login extraindo o token por JSON Extractor" (Aula 10).
- A API precisa ser exercitada do mesmo jeito por MockMvc, JMeter e interface.

## Requisitos e cenários
RF-01, RN-10, RNF-04, RNF-05 · CEN-02 · DR-02.

## Alternativas
- (a) Sessão com cookie (estado no servidor): funciona para a interface, mas complica JMeter e testes de API e exige proteção CSRF.
- (b) **JWT** assinado, enviado em `Authorization: Bearer`, sem estado no servidor.
- (c) HTTP Basic em cada requisição: envia a senha o tempo todo. Descartada.

## Decisão
(b) JWT com o perfil do usuário, expiração de 60 minutos (RNF-05, proposta), assinado com chave lida de variável de ambiente. Senhas com BCrypt.

## Consequências
- \+ Mesma forma de autenticar em testes, JMeter e interface.
- \+ Sem estado no servidor.
- − Um token emitido não pode ser revogado antes de expirar (sem lista de bloqueio nesta versão).
- − Exige cuidado para a chave não ir para o repositório.

## Riscos
- Chave de assinatura vazada permitiria forjar tokens → chave só em variável de ambiente.
- Token não revogável antes de expirar: um usuário desligado mantém acesso por até 60 min (aceite proposto em `docs/decisao-qualidade.md`).

## Evidência necessária
- Matriz de autorização testada célula por célula (Aula 12); 401 sem token; 403 para perfil errado; token expirado recusado (tempo via `Clock`).
- **Atendida:** `MatrizDeAutorizacaoIT` (88 células) e `ApiDeReservasIT.tokenVencidoResponde401` (token emitido com relógio 2 h atrás).

## Condição de revisão
Se surgir necessidade de revogar acesso imediatamente (ex.: usuário desligado), adicionar lista de bloqueio ou trocar para sessão.
