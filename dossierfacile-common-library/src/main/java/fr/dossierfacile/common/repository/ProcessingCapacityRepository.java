package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ProcessingCapacity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ProcessingCapacityRepository extends JpaRepository<ProcessingCapacity, Long> {
    ProcessingCapacity findByDate(LocalDate now);
}