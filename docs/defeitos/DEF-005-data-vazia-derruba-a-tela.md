# DEF-005 — Data vazia derruba a agenda e o relatório

| Campo | Valor |
|---|---|
| Identificador | DEF-005 |
| Situação | corrigido |
| Requisito afetado | RF-13 / RN-12 (interface com mensagens de erro compreensíveis) · RNF-09 |
| Encontrado por | Guilherme Viana Lopes, usando a aplicação (teste exploratório), em 2026-11-02 |
| Data | 2026-11-02 |

## Resumo observável
Na Agenda do dia, apagar a data troca a tela inteira por "Invalid time value". O próprio campo de data some junto, e só recarregando a página dá para voltar.

## Ambiente / versão
`v1.0.0-rc2` (`ae9d613`), Microsoft Edge no Windows 11, perfil Solicitante, aplicação local (`./mvnw spring-boot:run`).

## Pré-condições
Usuário logado; para o Relatório, perfil Responsável ou Administrador; para a Manutenção, Administrador.

## Passos mínimos para reproduzir
1. Entrar como Solicitante.
2. Na Agenda do dia, apagar a data do campo "Dia".

## Resultado obtido
A tela inteira vira um aviso "Invalid time value". O mesmo padrão aparece em outras três telas:

| Tela | Com data (ou hora) vazia |
|---|---|
| Agenda do dia | a tela some |
| Relatório | a tela some |
| Nova reserva | o botão "Ver recursos livres" não faz nada e não aparece mensagem |
| Manutenção | aparece "Invalid time value", em inglês |

## Resultado esperado / requisito
A tela continua visível. O usuário recebe uma mensagem em português ou o campo volta ao valor anterior (RF-13: "mensagens de erro compreensíveis").

## Evidências (sanitizadas)
Captura de tela enviada pelo relator (usuário de demonstração "Marina (Solicitante)", sem dado real). Os testes `def005_*` em `e2e/InterfaceE2EIT` reproduzem o defeito.

## Frequência, alcance e impacto
Sempre que um campo de data ou de hora fica vazio, nas quatro telas. Nenhum dado é gravado errado (a API não chega a ser chamada), mas o usuário fica sem a tela e sem explicação.

## Severidade (com justificativa)
**Média.** Não corrompe dados nem expõe informação, mas impede o uso da tela até recarregar e fere o RF-13, que é obrigatório.

## Prioridade (com justificativa)
**P1.** É visível em qualquer demonstração (o professor pode apagar a data na prova oral) e a correção é pequena.

## Causa raiz (5 porquês)
1. Por que a tela some? Porque um erro de JavaScript substitui todo o conteúdo pela mensagem do erro.
2. Por que houve erro? Porque `new Date('' + 'T12:00:00')` gera uma data inválida e `toISOString()` lança `RangeError: Invalid time value`.
3. Por que a data estava vazia? Porque o campo `type="date"` aceita ficar vazio e o código usava o valor sem conferir.
4. Por que o erro não foi tratado perto do campo? Porque a conversão de data ficava fora do `try` de cada tela, e só o tratador geral (`emSegundoPlano`) pegava o erro.
5. Condição controlável: **entrada do usuário convertida sem validação, e nenhum teste de interface com campo vazio.**

## Correção
- `instante()` em `static/js/app.js`: converte e, se a data for inválida, lança um erro com a mensagem "Informe uma data e um horário válidos.".
- Agenda: dia apagado é ignorado e o campo volta ao dia exibido.
- Nova reserva e Relatório: a conversão foi para dentro do `try` da tela, e a mensagem aparece no lugar do resultado.
- Manutenção: a mesma mensagem, em português.

## Critério de verificação
`e2e/InterfaceE2EIT`: os três testes `def005_*` ficam vermelhos sem a correção e verdes com ela; `./mvnw -B verify` verde.

## Rastreio (Aula 05: issue, commit, PR e teste de regressão)
| Item | Valor |
|---|---|
| Issue | [#17](https://github.com/guilhermevianacloud/organizacao-recursos/issues/17) |
| Commits do teste de regressão | `218eaa4` e `d91b6cb` |
| Commit da correção | `478ceaf` fix(DEF-005, DEF-006) |
| Pull request | [#19](https://github.com/guilhermevianacloud/organizacao-recursos/pull/19) |
| Teste de regressão | `e2e/InterfaceE2EIT.def005_*` (3 testes) |
