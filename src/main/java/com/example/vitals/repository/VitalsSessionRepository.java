package com.example.vitals.repository;

import com.example.vitals.entity.VitalsSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalsSessionRepository extends JpaRepository<VitalsSession, Long> {
}