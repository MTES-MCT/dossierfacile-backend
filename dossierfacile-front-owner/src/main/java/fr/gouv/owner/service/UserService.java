package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.PasswordRecoveryToken;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.owner.dto.UserDTO;
import fr.gouv.owner.repository.PasswordRecoveryTokenRepository;
import fr.gouv.owner.repository.UserRepository;
import fr.gouv.owner.security.ChangePasswordStatus;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String CHANGE_URL = "/changer-de-mot-de-passe/";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    @Autowired
    private PasswordRecoveryTokenService passwordRecoveryTokenService;
    @Autowired
    private MailService mailService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Value("${domain.url}")
    private String domainURL;
    @Value("${email.from}")
    private String emailFrom;
    @Value("${mailjet.template.id.recovery.password}")
    private Integer templateIDRecoveryPassword;
    @Value("${mailjet.template.id.apartmentsharing.joiner}")
    private Integer templateIDApartmentSharingJoiner;
    @Autowired
    private ModelMapper modelMapper;
    @Value("${authorize.domain.bo}")
    private String ad;

    public void launchPasswordRecoveryProcedure(String recoveryEmail) {
        User user = userRepository.findOneByEmail(recoveryEmail);
        if (null != user) {
            PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.savePasswordRecoveryToken(user);
            String tokenLink = domainURL + CHANGE_URL + passwordRecoveryToken.getToken();
            Map<String, String> variables = new HashMap<>();
            variables.put("firstName", user.getFirstName());
            variables.put("password_recovery_url", tokenLink);

            mailService.sendMailJetApi(emailFrom, null, user.getEmail(), user.getFullName(), null, null, null, null, null, null, null, variables, templateIDRecoveryPassword);
        }
    }

    public void launchPasswordCreationProcedureForApartmentSharingJoiner(String recoveryEmail, Tenant tenant) {
        User user = userRepository.findOneByEmail(recoveryEmail);
        if (null != user) {
            PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenService.savePasswordRecoveryToken(user);
            String tokenLink = domainURL + "/creer-un-mot-de-passe/" + passwordRecoveryToken.getToken();
            Map<String, String> variables = new HashMap<>();
            variables.put("password_recovery_url", tokenLink);
            variables.put("firstName", tenant.getFirstName());
            variables.put("lastName", tenant.getLastName());

            mailService.sendMailJetApi(emailFrom, null, user.getEmail(), user.getFullName(), null, null, null, null, null, null, null, variables, templateIDApartmentSharingJoiner);
        }
    }

    public User save(UserDTO userDTO) {
        User user = modelMapper.map(userDTO, User.class);

        return userRepository.save(user);
    }

    public User findOne(Long id) {
        return userRepository.getOne(id);
    }

    public User getUser(Principal principal) {
        return userRepository.findOneByEmail(principal.getName());
    }

    public User update(UserDTO userDTO) {
        User user = findOne(userDTO.getId());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        Set<UserRole> userRoleSet = new HashSet<>();
        for (Role role : userDTO.getRole()) {
            userRoleSet.add(new UserRole(user, role));
        }
        user.setUserRoles(userRoleSet);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public ChangePasswordStatus changePassword(String token, String password, String passwordConfirmation) {
        PasswordRecoveryToken passwordRecoveryToken = passwordRecoveryTokenRepository.findOneByToken(token);
        if (null == passwordRecoveryToken) {
            return ChangePasswordStatus.TOKEN_DOES_NOT_EXIST;
        }
        if (passwordRecoveryToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            return ChangePasswordStatus.TOKEN_TOO_OLD;
        }
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(passwordConfirmation) || !password.equals(passwordConfirmation)) {
            return ChangePasswordStatus.WRONG_PASSWORD;
        }

        User user = passwordRecoveryToken.getUser();

        user.setPassword(bCryptPasswordEncoder.encode(password));
        passwordRecoveryToken.setUser(null);
        userRepository.save(user);
        return ChangePasswordStatus.SUCCESS;
    }
}
