# Mutação manual das regras críticas

Aula 12 (Prompt 3). Para cada regra crítica, a regra foi **quebrada de propósito** no código de produção, os testes
ligados a ela foram executados e a mudança foi desfeita com `git restore` antes da próxima. Teste que continuasse
verde com a regra quebrada seria lacuna. Execução em 2026-11-01, branch `fase-7/carga-mutacao`, script de apoio
aplicando uma mutação por vez e conferindo depois que nenhum arquivo de `src/main` ficou alterado.

## Resultado
| # | Regra | Quebra feita | Testes executados | Resultado | Testes que falharam |
|---|---|---|---|---|---|
| M1 | Sobreposição (RN-02) | `Intervalo.sobrepoe`: `<` trocado por `<=` (reservas encostadas passam a conflitar) | `IntervaloTest`, `ServicoDeReservasIT` | **detectada** — 2 falhas | casos encostados de `IntervaloTest.Sobreposicao` (RN02-01 e RN02-04) |
| M2 | Dupla reserva (RN-03) | trava do recurso trocada por busca simples **e** migração V3 (exclusion constraint) removida | `DuplaReservaConcorrenciaIT`, `ProtecaoNoBancoIT` (com `clean`) | **detectada** — 7 falhas | 5 repetições da sala e o professor ("pedidos aceitos" > 1) e `ProtecaoNoBancoIT.bancoRecusaSobreposicao…` |
| M3 | Aprovação de restrito (RN-05) | todo recurso passa a ser aprovado na criação | `ReservaTest`, `CucumberIT` | **detectada** — 2 falhas + 2 erros | `ReservaTest.recursoRestritoNasceSolicitada…`, `…rn07_08…`, `…alterarHorarioDeRestrito…` (2) |
| M4 | Manutenção (RN-04) | verificação de manutenção removida do serviço | `ServicoDeReservasIT`, `CucumberIT` | **detectada** — 6 falhas | `ServicoDeReservasIT.EmManutencao` e os exemplos 409 da manutenção na feature |
| M5 | Autorização por posse (RN-10, IDOR) | conferência do dono removida | `MatrizDeAutorizacaoIT` | **detectada** — 2 falhas | células RES-06 e RES-07 (Solicitante mexendo na reserva de outro) |
| M6 | Autorização por perfil (RN-05) | `@PreAuthorize` da aprovação removido | `MatrizDeAutorizacaoIT`, `CucumberIT` | **detectada** — 2 falhas | células RES-08 como Solicitante e como Administrador |
| M7 | Auditoria (RN-09) | gravação da auditoria nas transições removida | `AuditoriaIT` | **detectada** — 3 falhas | `cancelamentoRegistra…`, `rn09_02_aprovacao…`, `rn09_06_…historicoPelaApi` |
| M8 | Máquina de estados (RN-07) | SOLICITADA passa a poder ir direto para EM_USO | `EstadoReservaTest`, `ReservaTest`, `CucumberIT` | **detectada** — 2 falhas | `EstadoReservaTest.soAsTransicoesDoPrdSaoPermitidas`, `ReservaTest.rn07_08…` |
| M9 | Término após início (RN-01) | duração zero passa a ser aceita | `IntervaloTest`, `ApiDeReservasIT` | **detectada** — 1 falha | `IntervaloTest.rn01_03_duracaoZeroEhRecusada` |
| M10 | Reserva iniciada não é apagada (RN-08) | `Reserva.podeSerApagada` passa a devolver sempre verdadeiro | `ReservaTest` | **detectada** — 3 falhas | `ReservaTest.rn08_reservaIniciadaNaoPodeSerApagada` (EM_USO, CONCLUIDA, NAO_COMPARECEU) |
| M11 | Quem pediu não aprova (RN-06) | conferência do solicitante removida de `ServicoDeReservas.aprovar` | `ServicoDeReservasTest`, `CucumberIT` | **detectada** — 1 falha | `ServicoDeReservasTest.rn06_quemPediuNaoAprovaAPropriaReservaNoServico` |

**Mutações detectadas: 11 de 11. Regras críticas da RTM com teste que detecta a quebra: 9 de 9 (100%).** Nenhuma mutação sobreviveu; nenhum teste novo foi necessário.

M1 a M10 foram executadas em 2026-11-01. A M11 entrou em 2026-11-02, depois que a revisão independente dos documentos apontou que a RN-06 aparecia como "coberta" sem mutação própria (ver a observação sobre M6).

## Como reproduzir
Na raiz do projeto, com o Docker ligado: `sh scripts/mutacao-manual.sh M5` (qualquer de M1 a M11). O script aplica a
quebra, roda os testes ligados a ela com `clean verify`, mostra o resultado e desfaz com `git restore`.

## Observações
- **M2, primeira tentativa inválida:** sem `clean`, a migração V3 apagada do código-fonte continuava em `target/classes`, e o banco
  dos testes ainda recebia a constraint — na prática só a trava tinha sido removida (o resultado foi o deadlock do DEF-002,
  "exceções inesperadas", e `ProtecaoNoBancoIT` passou). Refeita com `./mvnw clean verify`: agora sim, duplicatas e a falha do
  teste da constraint. O script passou a usar `clean` em todas as mutações.
- **M6 e defesa em profundidade:** o cenário BDD "Solicitante não aprova a própria reserva" continuou verde sem o `@PreAuthorize`,
  porque a regra RN-06 dentro do serviço também barra. A matriz detectou a mutação nas células RES-08. Por isso o cenário BDD
  não protege a RN-06 sozinho; quem detecta a quebra da regra no serviço é o teste de unidade (M11).
- **M7 e o teste de rollback:** `rn09_03_rollbackNaoDeixaAuditoriaOrfa` continua verde sem auditoria (nenhum registro = nenhum órfão).
  Ele prova outra propriedade (mesma transação); a ausência da gravação é pega pelos outros três testes de auditoria.
- Primeiras execuções de M1, M8 e M9 foram descartadas: o script chamava o Maven por `cmd.exe` fora da pasta do projeto e os
  testes nem rodaram. Corrigido e repetido; os números acima são das execuções válidas.
