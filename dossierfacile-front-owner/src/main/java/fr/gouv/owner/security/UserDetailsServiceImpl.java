package fr.gouv.owner.security;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.gouv.owner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        username = username.toLowerCase();
        User user = userRepository.findOneByEmail(username);
        if (user != null && user.getPassword() != null) {
            for (UserRole role : user.getUserRoles()) {
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getRole().name()));
            }
            return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthorities);
        } else {
            throw new UsernameNotFoundException(username);
        }
    }
}
