package fr.gouv.bo.security;

import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.model.RoleDTO;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleHierarchy roleHierarchy;

    public RoleService(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    public List<RoleDTO> getAvailableRoles(Role userRole) {
        if (userRole == null) {
            return List.of();
        }
        // ADMIN: peut créer ADMIN, MANAGER, OPERATOR, SUPPORT
        if (isRoleGreaterOrEqual(userRole, Role.ROLE_ADMIN)) {
            return List.of(
                    dto(Role.ROLE_ADMIN),
                    dto(Role.ROLE_MANAGER),
                    dto(Role.ROLE_OPERATOR),
                    dto(Role.ROLE_SUPPORT)
            );
        }
        // MANAGER: peut créer OPERATOR, SUPPORT (mais pas MANAGER)
        if (isRoleGreaterOrEqual(userRole, Role.ROLE_MANAGER)) {
            return List.of(
                    dto(Role.ROLE_OPERATOR),
                    dto(Role.ROLE_SUPPORT)
            );
        }
        // OPERATOR, SUPPORT et autres (TENANT, OWNER, PARTNER): rien
        return List.of();
    }

    public boolean isRoleGreaterOrEqual(Role userRole, Role role) {
        if (userRole == null || role == null) {
            return false;
        }
        // Depuis l'autorité userRole, calcule les autorités atteignables (incluant elle-même)
        var reachable = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority(userRole.name()))
        );
        String target = role.name();
        return reachable.stream().anyMatch(ga -> target.equals(ga.getAuthority()));
    }

    public Role getHighestRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return null;
        }
        // Retourne la liste des Roles qui sont atteignables depuis les autorités données
        Collection<? extends GrantedAuthority> reachable = roleHierarchy.getReachableGrantedAuthorities(new ArrayList<>(authorities));
        if (reachable == null || reachable.isEmpty()) {
            return null;
        }

        Set<String> reachableNames = reachable.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Détermine les autorités "parent" (celles qui ne sont pas impliquées par une autre)
        List<String> tops = new ArrayList<>();
        for (String candidate : reachableNames) {
            boolean impliedByAnother = false;
            for (String other : reachableNames) {
                if (candidate.equals(other)) continue;
                Collection<? extends GrantedAuthority> fromOther = roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority(other))
                );
                if (fromOther.stream().anyMatch(ga -> candidate.equals(ga.getAuthority()))) {
                    impliedByAnother = true;
                    break;
                }
            }
            if (!impliedByAnother) {
                tops.add(candidate);
            }
        }

        // Choisir la première autorité "top" qui existe dans l'enum Role
        for (String name : tops) {
            Role r = toRoleOrNull(name);
            if (r != null) {
                return r;
            }
        }
        // Sinon, choisir n'importe quelle autorité atteignable présente dans l'enum Role
        for (String name : reachableNames) {
            Role r = toRoleOrNull(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private Role toRoleOrNull(String name) {
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RoleDTO dto(Role role) {
        String display = role.name().replaceFirst("^ROLE_", "");
        return new RoleDTO(display, role);
    }

}
