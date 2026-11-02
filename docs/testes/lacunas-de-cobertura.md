# Lacunas de cobertura (JaCoCo)

Aula 07 (Prompt 1): ler o relatório por classe, listar cada linha ou branch não coberto, classificar em **risco**
ou **cosmética** e escrever teste **só para as lacunas de risco**, com asserção sobre comportamento.
Relatório: `target/site/jacoco/index.html` (gerado no `./mvnw -B verify` e publicado como artifact no CI).

## Gate
- Regra no `pom.xml` (JaCoCo 0.8.15, fase `verify`): `BUNDLE`, **linhas ≥ 0.80**, **branches ≥ 0.70**. Nenhuma classe excluída.
- **Prova de que o gate reprova** (2026-11-01): com a meta de linhas trocada temporariamente para 0.99,
  `./mvnw -B jacoco:check@report-and-check` terminou em `BUILD FAILURE` — "lines covered ratio is 0.95, but expected
  minimum is 0.99"; restaurada a meta de 0.80, voltou a `All coverage checks have been met`. O `pom.xml` não guardou a mudança.

## Medições
| Momento | Linhas | Branches |
|---|---|---|
| Antes da análise (Fase 5 completa) | 95,1% (565/594) | 83,0% (117/141) |
| Depois dos testes das lacunas de risco | 98,0% (582/594) | 95,0% (134/141) |
| Versão avaliada (depois do DEF-004, que acrescentou uma linha) | 98,0% (583/595) | 95,0% (134/141) |

## Lacunas de risco (ganharam teste)
| Classe e trecho | Por que é risco | Teste criado |
|---|---|---|
| `ServicoDeReservas.aprovar` — dono não aprova (RN-06) | Regra crítica; a camada de perfil barrava antes e o código da regra nunca era exercido | `ServicoDeReservasTest.rn06_quemPediuNaoAprovaAPropriaReservaNoServico` |
| `ServicoDeReservas.gravar` — violação da constraint vira conflito | Com a trava (ADR-002) a constraint não dispara no caminho normal; se a trava faltar, sem tradução o usuário receberia 500 | `ServicoDeReservasTest.violacaoDaConstraintDeSobreposicaoViraConflitoDeHorario` e `…outraViolacaoDeIntegridadeNaoEhDisfarcadaDeConflito` (Mockito, unidade isolada) |
| `Reserva.alterarHorario` — restrito aprovado e reserva iniciada não mudam | Regra de negócio sem nenhum teste | `ReservaTest` (4 testes, 6 casos) |
| `ServicoDeReservas.recursoDa` / `ReservaController.alterar` — conflito na alteração | Conflito ao alterar devia virar 409 e contar como conflito evitado | `ApiDeReservasIT.alterarParaHorarioOcupadoResponde409ERegistraConflitoEvitado` |
| `ServicoDeRelatorios` — reserva que começa antes do período | Só a borda final estava testada (D-14) | `RelatorioDeUtilizacaoIT.reservaQueComecaAntesDoPeriodoContaSoAsHorasDentro` |
| `NotificadorHttp` — 200 sem identificador | Contaria como enviada sem confirmação do provedor | `NotificadorHttpIT.int01b_respostaSemIdentificadorNaoContaComoEnviada` |
| `ConfiguracaoDeSeguranca.chaveDeAssinatura` — chave curta | Chave fraca permitiria forjar tokens (RNF-05) | `ConfiguracaoDeSegurancaTest` (fronteira 31/32/33 e chave aleatória) |
| `ServicoDeRecursos.obter` — recurso inexistente | 404 sem teste na API | `ApiDeReservasIT.recursoInexistenteResponde404` |

## Lacunas cosméticas (sem teste, com o motivo)
| Classe e trecho | Motivo |
|---|---|
| `OrganizacaoRecursosApplication.main` | Ponto de entrada; exercido ao subir a aplicação, não pelos testes |
| `AuditoriaReserva` — getters de id | Não usados por nenhuma regra |
| `ServicoDeRelatorios` — `conflitos == null` | `count(*)` nunca devolve nulo; defesa sem caminho real |
| `ServicoDeRecursos.contem` — trecho em branco | Equivale a "sem filtro"; já coberto pelo caso nulo |
| `DadosDeDemonstracao` — banco já populado | Caminho de conveniência de demonstração |
| `TratamentoDeErros.inesperado` (500 genérico) e `RespostasDeSeguranca` parcial | Rede de segurança; o conteúdo do 500 é fixo e sem dado interno |
| `ConfiguracaoDeNotificacao.OuvinteDeReservas` — `catch` | O notificador já não lança exceção; defesa extra |
| `NotificadorHttp` — log de timeout em milissegundos | Só formatação de log |

## O que a cobertura não prova
Cobertura mostra que o código **rodou**, não que o resultado foi **verificado**. Por isso a confirmação de que os
testes protegem as regras críticas vem da **mutação manual** (Fase 7, `docs/testes/mutacao-manual.md`).
