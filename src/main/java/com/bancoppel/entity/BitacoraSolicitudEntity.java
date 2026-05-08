package com.bancoppel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orquestador_bitacora_solicitud")
@Getter
@Setter
public class BitacoraSolicitudEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(name = "num_solicitud", length = 100)
    private String numSolicitud; // Ideal para UUIDs o folios de rastreo

    @Column(name = "num_cliente", length = 50)
    private String numCliente;

    @Column(name = "telefono")
    private String telefono; // Tal cual el diagrama (INT)

    @Column(name = "tel_cifrado", length = 255)
    private String telCifrado;

    // Relación al Catálogo de Estatus
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estatus", nullable = false)
    private CatEstatusEntity estatus;

    // Usamos TEXT porque los JSON en PostgreSQL pueden superar los 255 caracteres del VARCHAR default
    @Column(name = "json_solicitud", columnDefinition = "TEXT")
    private String jsonSolicitud;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bitacoraSolicitud", cascade = CascadeType.ALL)
    private List<BitacoraRespuestaEntity> respuestas;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}