package com.object.checker.repo.apartment;

import com.object.checker.model.apartment.ApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface apartmentRepo extends JpaRepository<ApartmentSharing, Long> {

}
