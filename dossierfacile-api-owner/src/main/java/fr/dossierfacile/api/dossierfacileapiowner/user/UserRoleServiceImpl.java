package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {
    private final UserRoleRepository userRoleRepository;

    @Override
    public void createRole(Owner owner) {
        List<UserRole> userRoles = userRoleRepository.findByUser(owner);
        if (userRoles.isEmpty()) {
            userRoles = Collections.singletonList(new UserRole(owner, Role.ROLE_OWNER));
            userRoleRepository.saveAll(userRoles);
        }
    }
}
