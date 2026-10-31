package br.senac.organizacaorecursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** Reserva de um único recurso (D-07) num intervalo [início, término). */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant termino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    private String motivo;

    @Version
    private Long versao;

    protected Reserva() {
    }

    private Reserva(Recurso recurso, Long solicitanteId, Intervalo intervalo) {
        this.recurso = recurso;
        this.solicitanteId = solicitanteId;
        this.inicio = intervalo.inicio();
        this.termino = intervalo.termino();
        this.estado = EstadoReserva.SOLICITADA;
    }

    public static Reserva solicitar(Recurso recurso, Long solicitanteId, Intervalo intervalo) {
        return new Reserva(recurso, solicitanteId, intervalo);
    }

    public void transitarPara(EstadoReserva novo) {
        this.estado = novo;
    }

    public boolean podeSerApagada() {
        return true;
    }

    public Intervalo intervalo() {
        return new Intervalo(inicio, termino);
    }

    public Long getId() {
        return id;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
}
