package com.bancoppel.repository;

import com.bancoppel.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Integer> {

    Optional<ProveedorEntity> findByNombreAndActivoTrue(String nombre);

}
