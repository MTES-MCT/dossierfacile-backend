package fr.dossierfacile.migrate.user.controller;

import fr.dossierfacile.migrate.user.model.UserInfo;
import fr.dossierfacile.migrate.user.model.UserInfoPassword;
import fr.dossierfacile.migrate.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
public class MigrateUserToKeycloakController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/migrate/{username}")
    public ResponseEntity<UserInfo> getUserInfo(@PathVariable String username) {
        var user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException(username));
        var userInfo = UserInfo.builder()
                .username(user.getEmail())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailVerified(true)
                .enabled(user.isEnabled())
                .roles(Collections.singletonList("TENANT"))
                .groups(Collections.emptyList())
                .requiredActions(Collections.emptyList())
                .build();
        return ok(userInfo);
    }

    @PostMapping("/migrate/{username}")
    public ResponseEntity<Void> checkPassword(@PathVariable String username, @RequestBody UserInfoPassword userInfoPassword) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, userInfoPassword.getPassword()));
            return ok().build();
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }
    }
}
