package br.senac.organizacaorecursos.api;

import br.senac.organizacaorecursos.dominio.EstadoReserva;
import br.senac.organizacaorecursos.dominio.Manutencao;
import br.senac.organizacaorecursos.dominio.Recurso;
import br.senac.organizacaorecursos.dominio.Reserva;
import br.senac.organizacaorecursos.dominio.TipoRecurso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Formatos de entrada e saída da API. Mensagens de validação em português (RN-12). */
public final class Dtos {

    private Dtos() {
    }

    public record LoginPedido(
            @NotBlank(message = "é obrigatório") @Email(message = "deve ser um e-mail válido") String email,
            @NotBlank(message = "é obrigatória") String senha) {
    }

    public record TokenResposta(String token, String tipo, Instant expiraEm, String perfil, String nome) {
    }

    public record RecursoPedido(
            @NotNull(message = "é obrigatório (SALA, MATERIAL ou PROFESSOR)") TipoRecurso tipo,
            @NotBlank(message = "é obrigatório") @Size(max = 255, message = "deve ter até 255 caracteres") String nome,
            @PositiveOrZero(message = "não pode ser negativa") Integer capacidade,
            @Size(max = 255, message = "deve ter até 255 caracteres") String localizacao,
            @Size(max = 255, message = "deve ter até 255 caracteres") String competencias,
            boolean restrito) {
    }

    public record RecursoResposta(Long id, TipoRecurso tipo, String nome, Integer capacidade, String localizacao,
                                  String competencias, boolean restrito, boolean ativo) {
        static RecursoResposta de(Recurso r) {
            return new RecursoResposta(r.getId(), r.getTipo(), r.getNome(), r.getCapacidade(), r.getLocalizacao(),
                    r.getCompetencias(), r.isRestrito(), r.isAtivo());
        }
    }

    public record ManutencaoPedido(
            @NotNull(message = "é obrigatório") Long recursoId,
            @NotNull(message = "é obrigatório") Instant inicio,
            @NotNull(message = "é obrigatório") Instant termino,
            @Size(max = 255, message = "deve ter até 255 caracteres") String motivo) {
    }

    public record ManutencaoResposta(Long id, Long recursoId, String recursoNome, Instant inicio, Instant termino,
                                     String motivo) {
        static ManutencaoResposta de(Manutencao m) {
            return new ManutencaoResposta(m.getId(), m.getRecurso().getId(), m.getRecurso().getNome(),
                    m.intervalo().inicio(), m.intervalo().termino(), m.getMotivo());
        }
    }

    public record ReservaPedido(
            @NotNull(message = "é obrigatório") Long recursoId,
            @NotNull(message = "é obrigatório") Instant inicio,
            @NotNull(message = "é obrigatório") Instant termino,
            @Size(max = 255, message = "deve ter até 255 caracteres") String motivo) {
    }

    public record AlteracaoPedido(
            @NotNull(message = "é obrigatório") Instant inicio,
            @NotNull(message = "é obrigatório") Instant termino) {
    }

    public record ReservaResposta(Long id, Long recursoId, String recursoNome, TipoRecurso recursoTipo,
                                  boolean recursoRestrito, Long solicitanteId, Instant inicio, Instant termino,
                                  EstadoReserva estado, String motivo) {
        static ReservaResposta de(Reserva r) {
            Recurso recurso = r.getRecurso();
            return new ReservaResposta(r.getId(), recurso.getId(), recurso.getNome(), recurso.getTipo(),
                    recurso.isRestrito(), r.getSolicitanteId(), r.intervalo().inicio(), r.intervalo().termino(),
                    r.getEstado(), r.getMotivo());
        }
    }
}
