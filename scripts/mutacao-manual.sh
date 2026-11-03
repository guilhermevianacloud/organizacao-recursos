#!/bin/sh
# Mutação manual (Aula 12): aplica UMA quebra de regra, roda os testes ligados a ela e desfaz com git restore.
# Uso (na raiz do projeto, com Docker ligado): sh scripts/mutacao-manual.sh <M1..M11>
# Registro dos resultados: docs/testes/mutacao-manual.md. O código sempre volta ao original (git restore).
set -u
P="$(cd "$(dirname "$0")/.." && pwd)"
J="$P/src/main/java/br/senac/organizacaorecursos"
cd "$P" || exit 1
ID="$1"
UNIT="NenhumTeste"; IT="NenhumTeste"
case "$ID" in
  M1) REGRA="Sobreposição (RN-02): '<' trocado por '<=' (reservas encostadas passam a conflitar)"
      sed -i 's/        return inicio.isBefore(outro.termino) \&\& outro.inicio.isBefore(termino);/        return !inicio.isAfter(outro.termino) \&\& !outro.inicio.isAfter(termino);/' "$J/dominio/Intervalo.java"
      UNIT="IntervaloTest"; IT="ServicoDeReservasIT" ;;
  M2) REGRA="Dupla reserva (RN-03): sem a trava do recurso e sem a exclusion constraint (migração V3 removida)"
      sed -i 's/        return recursos.buscarTravandoParaReserva(recursoId)/        return recursos.findById(recursoId)/' "$J/servico/ServicoDeReservas.java"
      rm "$P/src/main/resources/db/migration/V3__protecao_contra_dupla_reserva.sql"
      IT="DuplaReservaConcorrenciaIT,ProtecaoNoBancoIT" ;;
  M3) REGRA="Aprovação de restrito (RN-05): todo recurso passa a ser aprovado automaticamente"
      sed -i 's/        if (!recurso.isRestrito()) {/        if (true) {/' "$J/dominio/Reserva.java"
      UNIT="ReservaTest"; IT="CucumberIT" ;;
  M4) REGRA="Manutenção (RN-04): verificação de manutenção removida"
      sed -i 's/        if (manutencoes.existeSobreposicao(recursoId, intervalo.inicio(), intervalo.termino())) {/        if (false) {/' "$J/servico/ServicoDeReservas.java"
      IT="ServicoDeReservasIT,CucumberIT" ;;
  M5) REGRA="Autorização por posse (RN-10, IDOR): conferência do dono removida"
      sed -i 's/        if (!reserva.pertenceA(usuarioId)) {/        if (false) {/' "$J/servico/ServicoDeReservas.java"
      IT="MatrizDeAutorizacaoIT" ;;
  M6) REGRA="Autorização por perfil (RN-05): @PreAuthorize da aprovação removido"
      sed -i '/@PostMapping("\/{id}\/aprovacao")/{n;d}' "$J/api/ReservaController.java"
      IT="MatrizDeAutorizacaoIT,CucumberIT" ;;
  M7) REGRA="Auditoria (RN-09): gravação da auditoria nas transições removida"
      sed -i 's/        auditar(salva, usuarioId, anterior, novo, motivo);/        \/\/ auditoria removida (mutação)/' "$J/servico/ServicoDeReservas.java"
      IT="AuditoriaIT" ;;
  M8) REGRA="Máquina de estados (RN-07): SOLICITADA passa a poder ir direto para EM_USO"
      sed -i 's/            case SOLICITADA -> EnumSet.of(APROVADA, REJEITADA, CANCELADA);/            case SOLICITADA -> EnumSet.of(APROVADA, REJEITADA, CANCELADA, EM_USO);/' "$J/dominio/EstadoReserva.java"
      UNIT="EstadoReservaTest,ReservaTest"; IT="CucumberIT" ;;
  M9) REGRA="Término após início (RN-01): duração zero passa a ser aceita"
      sed -i 's/        if (!termino.isAfter(inicio)) {/        if (termino.isBefore(inicio)) {/' "$J/dominio/Intervalo.java"
      UNIT="IntervaloTest"; IT="ApiDeReservasIT" ;;
  M10) REGRA="Reserva iniciada não é apagada (RN-08): podeSerApagada sempre verdadeiro"
      sed -i "s/        return !estado.iniciada();/        return true;/" "$J/dominio/Reserva.java"
      UNIT="ReservaTest" ;;
  M11) REGRA="Quem pediu não aprova (RN-06): conferência do solicitante na aprovação removida"
      sed -i 's/        if (reserva.pertenceA(responsavelId)) {/        if (false) {/' "$J/servico/ServicoDeReservas.java"
      UNIT="ServicoDeReservasTest"; IT="CucumberIT" ;;
  *) echo "mutação desconhecida: $ID"; exit 2 ;;
esac
echo "### $ID — $REGRA"
MUDOU=$(git status --porcelain src/main | wc -l)
echo "arquivos alterados pela mutação: $MUDOU"
git diff --stat src/main | tail -1
if [ "$MUDOU" -eq 0 ]; then echo "ERRO: a mutação não alterou nada"; exit 3; fi
echo "testes: unidade=$UNIT integração=$IT"
./mvnw -B clean verify -Djacoco.skip=true -Dtest="$UNIT" -Dit.test="$IT" -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false > "${TMPDIR:-/tmp}/mutacao-$ID.log" 2>&1
grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD (SUCCESS|FAILURE)|COMPILATION ERROR' "${TMPDIR:-/tmp}/mutacao-$ID.log" | sed 's/^\[[A-Z]*\] //'
grep -E '^\[ERROR\]   [A-Za-z]+.*:[0-9]+ ' "${TMPDIR:-/tmp}/mutacao-$ID.log" | head -4 | cut -c1-200
git restore src/main
echo "restaurado: $(git status --porcelain src/main | wc -l) arquivos ainda alterados"
