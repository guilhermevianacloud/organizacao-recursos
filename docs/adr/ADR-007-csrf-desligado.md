# ADR-007 — Proteção CSRF desligada na API stateless

Status: proposta (2026-11-01; escrita pelo Claude a partir do apontamento `java:S4502` do SonarCloud) · aguardando revisão de Guilherme Viana Lopes, que marca o apontamento no SonarCloud se concordar

## Contexto e forças
- A primeira análise do SonarCloud apontou `java:S4502` (segurança, severidade alta) em `ConfiguracaoDeSeguranca`: "Make sure disabling Spring Security's CSRF protection is safe here."
- CSRF é o ataque em que outro site faz o navegador da vítima enviar uma requisição ao nosso sistema. Ele só funciona quando a credencial vai **sozinha** em cada requisição, como um cookie de sessão.
- Aqui a autenticação é por JWT (ADR-003): sem sessão no servidor (`STATELESS`), sem cookie. A interface (ADR-006) guarda o token no `sessionStorage` e o envia no cabeçalho `Authorization: Bearer`, que só o nosso JavaScript monta.
- Meta da especificação: 0 bugs ou vulnerabilidades críticas.

## Requisitos e cenários
- RNF-05 (senhas e tokens protegidos) e a meta "0 vulnerabilidades críticas" (RNF-10).
- CEN-02 (acesso indevido): a escrita sem o cabeçalho `Authorization` é recusada.

## Alternativas
- (a) Ligar o CSRF do Spring: exigiria um token CSRF em toda requisição de escrita (interface, testes, JMeter) sem proteger nada, porque não há cookie para o atacante aproveitar.
- (b) **Manter desligado, justificar no código e neste ADR**, e registrar a revisão no SonarCloud.

## Decisão
(b), se o Guilherme concordar. Comentário na própria linha aponta para este ADR.

## Consequências
- \+ Nenhuma peça a mais na interface, nos testes ou no JMeter.
- − A segurança passa a depender de o token **nunca** ir para um cookie. Se isso mudar, o CSRF tem de voltar.
- − O token no `sessionStorage` pode ser lido por um script injetado (XSS). Mitigação: todo texto da API passa pela função de escape da interface (ADR-006).

## Riscos
- A segurança passa a depender de o token nunca ir para um cookie; uma mudança assim reabriria o CSRF sem nenhum teste falhar → condição de revisão abaixo.
- Script injetado (XSS) poderia ler o token do `sessionStorage` → escape de todo texto vindo da API (ADR-006).

## Evidência
- `MatrizDeAutorizacaoIT` e `ApiDeReservasIT`: requisição sem o cabeçalho `Authorization` recebe 401. Sem o cabeçalho, que outro site não consegue montar, a escrita não passa.

## Condição de revisão
Qualquer mudança que leve a credencial para um cookie (sessão, "lembrar-me", token em cookie) reabre esta decisão.
