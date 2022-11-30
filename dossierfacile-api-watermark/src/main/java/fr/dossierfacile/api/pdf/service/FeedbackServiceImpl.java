package fr.dossierfacile.api.pdf.service;

import fr.dossierfacile.api.pdf.repository.FeedbackRepository;
import fr.dossierfacile.api.pdf.service.interfaces.FeedbackService;
import fr.dossierfacile.common.entity.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Override
    public void saveFeedback(boolean value) {
        Feedback feedback = Feedback.builder().value(value).build();
        feedbackRepository.save(feedback);
    }
}
