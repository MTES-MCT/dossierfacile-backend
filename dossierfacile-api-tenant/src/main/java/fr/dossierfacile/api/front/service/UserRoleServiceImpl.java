package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.UserRoleRepository;
import fr.dossierfacile.api.front.service.interfaces.UserRoleService;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserRole;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    private final UserRoleRepository userRoleRepository;

    @Override
    public void createRole(Tenant tenant) {
        List<UserRole> userRoles = userRoleRepository.findByUser(tenant).orElse(Collections.singletonList(new UserRole(tenant)));
        userRoleRepository.saveAll(userRoles);
    }
}
