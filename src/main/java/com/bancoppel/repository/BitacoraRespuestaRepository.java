package com.bancoppel.repository;

import com.bancoppel.entity.BitacoraRespuestaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BitacoraRespuestaRepository extends JpaRepository<BitacoraRespuestaEntity, Integer> {

    // Busca la respuesta exitosa más reciente de un proveedor específico para un teléfono
    Optional<BitacoraRespuestaEntity> findFirstByTelefonoAndProveedor_NombreAndFlagConsultaSatisfactoriaTrueAndCreatedAtAfterOrderByCreatedAtDesc(
            String telefono,
            String nombreProveedor,
            LocalDateTime fechaLimite
    );

}
