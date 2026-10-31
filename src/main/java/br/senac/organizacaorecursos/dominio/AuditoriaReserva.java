package br.senac.organizacaorecursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** RN-09: registro imutável de uma mudança da reserva. Não há setter nem rota para alterar ou apagar. */
@Entity
@Immutable
@Table(name = "auditoria_reserva")
public class AuditoriaReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Instant instante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false)
    private EstadoReserva estadoNovo;

    @Column(nullable = false)
    private String motivo;

    protected AuditoriaReserva() {
    }

    public AuditoriaReserva(Long reservaId, Long usuarioId, Instant instante, EstadoReserva estadoAnterior,
                            EstadoReserva estadoNovo, String motivo) {
        this.reservaId = reservaId;
        this.usuarioId = usuarioId;
        this.instante = instante;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getInstante() {
        return instante;
    }

    public EstadoReserva getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoReserva getEstadoNovo() {
        return estadoNovo;
    }

    public String getMotivo() {
        return motivo;
    }
}
