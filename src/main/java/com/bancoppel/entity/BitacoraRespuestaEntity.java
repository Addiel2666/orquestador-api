package com.bancoppel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "orquestador_bitacora_respuesta")
@Getter
@Setter
public class BitacoraRespuestaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    // --- NUEVO CAMPO: Autorreferencia ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_respuesta_origen", nullable = true) // Es nullable porque las originales no tienen padre
    private BitacoraRespuestaEntity respuestaOrigen;

    // Llave Foránea hacia Solicitud
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private BitacoraSolicitudEntity bitacoraSolicitud;

    // Llave Foránea hacia Proveedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private ProveedorEntity proveedor;

    @Column(name = "num_cliente", length = 50)
    private String numCliente;

    @Column(name = "telefono", length = 20)
    private String telefono; // Tal cual el diagrama (VARCHAR en esta tabla)

    @Column(name = "tel_cifrado", length = 255)
    private String telCifrado;

    @Column(name = "json_respuesta", columnDefinition = "TEXT")
    private String jsonRespuesta;

    @Column(name = "codigo_estatus")
    private Integer codigoEstatus; // 200, 400, 500, etc.

    @Column(name = "flag_consulta_satisfactoria")
    private Boolean flagConsultaSatisfactoria;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}