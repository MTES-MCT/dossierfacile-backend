package com.object.checker.repo.document;

import com.object.checker.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface documentRepo extends JpaRepository<Document, Long> {
}
