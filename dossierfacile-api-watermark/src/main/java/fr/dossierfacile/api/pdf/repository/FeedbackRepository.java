package fr.dossierfacile.api.pdf.repository;

import fr.dossierfacile.common.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
