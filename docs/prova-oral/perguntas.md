# Banco de perguntas da banca

Aula 13 (Prompt 2). Quinze perguntas **deste projeto**, com o arquivo onde está a resposta e como seria uma resposta
**fraca**. As respostas não estão aqui de propósito: cada integrante responde sem ler. A distribuição faz cada pessoa
responder sobre partes que não escreveu nem revisou (o código foi gerado com agente de IA; ver `README.md`, "Uso de IA").

| # | Pergunta | Onde está a resposta | Resposta fraca | Quem responde |
|---|---|---|---|---|
| 1 | Qual mecanismo impede a dupla reserva e qual teste prova isso? | `docs/adr/ADR-002-protecao-dupla-reserva.md`; `DuplaReservaConcorrenciaIT`; `ProtecaoNoBancoIT` | "O sistema verifica se a sala está livre antes de salvar." (é exatamente o que falhou no DEF-001) | Rafael |
| 2 | Por que houve mock num ponto e Testcontainers em outro? | `ServicoDeReservasTest`; `TestcontainersConfiguration`; `docs/plano-de-teste.md` | "Mock é mais rápido." (não cita a regra da especificação nem o caminho que só o mock alcança) | Gustavo |
| 3 | O que o p95 medido permite e o que ele não permite afirmar? | `docs/performance/resultado-01.md` | "O sistema é rápido" ou "o sistema aguenta produção." | Guilherme |
| 4 | Qual teste falha se a aprovação de restrito for removida? | `docs/testes/mutacao-manual.md` (M3, M6); `aprovacao-bloqueio.feature`; `MatrizDeAutorizacaoIT` | "Algum teste de reserva." (sem nome nem evidência) | Rafael |
| 5 | Por que a reserva das 8h às 10h não conflita com a das 10h às 12h? Onde isso está decidido e testado? | `docs/adr/ADR-004-intervalos-semiabertos.md`; `IntervaloTest` (RN02-01, RN02-04) | "Porque não sobrepõe." (sem dizer intervalo semiaberto nem a decisão) | Gustavo |
| 6 | O DEF-002 apareceu "às vezes". Vocês rodaram de novo até passar? O que era e como foi resolvido? | `docs/defeitos/DEF-002-deadlock-na-disputa-pelo-mesmo-recurso.md` | "Foi instabilidade do teste." | Guilherme |
| 7 | Como vocês sabem que a auditoria é gravada na mesma transação da mudança de estado? | `AuditoriaIT.rn09_03_rollbackNaoDeixaAuditoriaOrfa`; `ServicoDeReservas.transitar` | "Porque está no mesmo método." (sem a prova do rollback) | Rafael |
| 8 | A cobertura é 98%. O que isso prova e o que não prova? | `docs/testes/lacunas-de-cobertura.md`; `docs/testes/mutacao-manual.md` | "Prova que está quase tudo testado." | Gustavo |
| 9 | Um Solicitante consegue cancelar a reserva de outro trocando o id na URL? Mostre. | `docs/seguranca/matriz-autorizacao.md` (RES-06, RES-07); `MatrizDeAutorizacaoIT`; mutação M5 | "Não, porque precisa de login." (confunde autenticação com autorização) | Guilherme |
| 10 | De onde saíram as 3,5 h da Sala A no teste do relatório? | comentário em `RelatorioDeUtilizacaoIT` | "Do resultado da API." (o professor pede o cálculo à mão) | Rafael |
| 11 | Se o provedor de notificação cair, a reserva some? Por quê? | `docs/adr/ADR-005-notificacao-porta-adaptador.md`; `NotificacaoAposCommitIT`; `NotificadorHttpIT` (INT-03, INT-04) | "Não, porque tem try/catch." (sem falar de envio depois do commit) | Gustavo |
| 12 | Por que o CI roda `verify` e não `test`? O que acontece se um teste falhar num pull request? | `.github/workflows/ci.yml`; branch protection; runs no `README.md` | "Porque é o padrão." | Guilherme |
| 13 | Qual a diferença entre 401 e 403 no projeto? Dê um exemplo de cada. | `docs/seguranca/matriz-autorizacao.md`; `RespostasDeSeguranca`; `ApiDeReservasIT.tokenAdulteradoResponde401` | "Os dois são erro de permissão." | Rafael |
| 14 | O erro de validação vaza detalhe interno? Como vocês garantem? | `TratamentoDeErros`; `ApiDeReservasIT.semDetalheInterno` | "Não vaza porque o Spring cuida." | Gustavo |
| 15 | Qual decisão de arquitetura concentra mais trade-offs e qual o custo dela? | `docs/arquitetura.md` (seção 11); ADR-002; resultado de carga | "Usar Spring Boot." (decisão sem trade-off registrado) | Guilherme |

## Como usar
1. Ensaio cronometrado com o agente (Aula 13, Prompt 3): uma pergunta por vez, cinco por pessoa, resposta sem ler.
2. Critérios de 0 a 2 em cada resposta: **correção técnica**, **evidência citada** (arquivo, teste ou relatório) e **domínio de autoria**.
3. Registrar a pontuação real e as lacunas em [`ensaio.md`](ensaio.md).
