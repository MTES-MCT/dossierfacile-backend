package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.repository.UserRepository;
import fr.dossierfacile.api.front.repository.UserRoleRepository;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;


    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Username: " + username + " not found"));
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        List<UserRole> userRoles = userRoleRepository.findByUser(user).orElse(new ArrayList<>());
        for (UserRole role : userRoles) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getRole().name()));
        }
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), user.isEnabled(), true, true, true, grantedAuthorities);
    }
}
