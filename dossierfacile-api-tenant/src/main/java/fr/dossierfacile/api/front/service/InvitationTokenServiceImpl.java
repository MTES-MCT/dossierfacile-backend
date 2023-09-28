package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.InvitationTokenRepository;
import fr.dossierfacile.api.front.service.interfaces.InvitationTokenService;
import fr.dossierfacile.common.entity.InvitationToken;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class InvitationTokenServiceImpl implements InvitationTokenService {
    private InvitationTokenRepository invitationTokenRepository;
    @Value("${invitation.delay.before.expiration.hours}")
    private Long delayBeforeExpirationInHours;

    @Override
    public List<InvitationToken> findExpiredInvitation() {
        return invitationTokenRepository.findAllByCreatedDateBefore(LocalDateTime.now().minusHours(delayBeforeExpirationInHours));
    }

    @Override
    public void delete(InvitationToken invitationToken) {
        invitationTokenRepository.delete(invitationToken);
    }
}
