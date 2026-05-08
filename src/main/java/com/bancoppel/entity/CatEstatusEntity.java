package com.bancoppel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cat_estatus")
@Getter
@Setter
public class CatEstatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estatus") // En el diagrama dice idEstatus, pero estandarizamos a snake_case en BD
    private Integer idEstatus;

    @Column(name = "descripcion", length = 50)
    private String descripcion;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;
}