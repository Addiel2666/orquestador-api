package com.bancoppel.repository;

import com.bancoppel.entity.BitacoraSolicitudEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BitacoraSolicitudRepository extends JpaRepository<BitacoraSolicitudEntity, Integer> {

    /**
     * Esta consulta busca la última solicitud exitosa para un teléfono específico
     * dentro de un rango de tiempo (los últimos 20 días).
     * Spring traduce este nombre largo a un SELECT con WHERE telefono = ? AND created_at > ?
     */
    Optional<BitacoraSolicitudEntity> findFirstByTelefonoAndCreatedAtAfterOrderByCreatedAtDesc(
            String telefono,
            LocalDateTime fechaLimite
    );
}