package fr.gouv.owner.security;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RedirectAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RedirectAuthenticationSuccessHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        HttpSession session = request.getSession();
        User user = userRepository.findOneByEmail(authentication.getName());
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        Owner owner = null;
        String redirectUrl = null;

        if (user instanceof Owner) {
            log.info("onAuthenticationSuccess logged Owner, id: {}", user.getId());
            owner = (Owner) user;
        }

        if (owner != null && owner.getStepRegisterOwner() == StepRegisterOwner.STEP2) {
            redirectUrl = "/registerOwner/step3/" + owner.getSlug();
        }
        if (null != session) {
            String tenantToken = (String) session.getAttribute("token");
            if (null != tenantToken) {
                session.removeAttribute("token");
            }
            String ownerToken = (String) session.getAttribute("ownerToken");
            if (null != ownerToken) {
                session.removeAttribute("ownerToken");
            }
        }

        if (redirectUrl == null) {
            if (owner != null) {
                redirectUrl = "/proprietaire/ma-propriete";
            } else {
                redirectUrl = "/";
            }
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }


}
