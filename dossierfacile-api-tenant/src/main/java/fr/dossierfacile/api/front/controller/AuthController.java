package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.repository.TenantRepository;
import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.security.jwt.AuthenticationRequest;
import fr.dossierfacile.api.front.security.jwt.JwtTokenProvider;
import fr.dossierfacile.api.front.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.api.front.service.interfaces.SourceService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository users;
    private final TenantRepository tenantRepository;
    private final PartnerCallBackService partnerCallBackService;
    private final SourceService sourceService;


    @PostMapping("")
    public ResponseEntity<Map<String, String>> auth(@RequestBody AuthenticationRequest data) {
        try {
            String username = data.getUsername();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
            String token = jwtTokenProvider.createToken(username, this.users.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found")).getUserRoles().stream().map(UserRole::getRole).map(Role::name).collect(Collectors.toList()));
            Map<String, String> model = new HashMap<>();
            model.put("username", username);
            model.put("token", token);

            Tenant tenant = tenantRepository.findByEmail(data.getUsername()).orElseThrow(() -> new TenantNotFoundException(data.getUsername()));
            UserApi userApi = sourceService.findOrCreate(data.getSource());
            partnerCallBackService.registerTenant(data.getInternalPartnerId(),tenant,userApi);
            return ok(model);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }
    }
}
