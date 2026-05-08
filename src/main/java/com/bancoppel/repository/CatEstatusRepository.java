package com.bancoppel.repository;

import com.bancoppel.entity.CatEstatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatEstatusRepository extends JpaRepository<CatEstatusEntity, Integer> {
}