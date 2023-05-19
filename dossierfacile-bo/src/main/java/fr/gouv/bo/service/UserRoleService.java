package fr.gouv.bo.service;

import com.drew.lang.annotations.NotNull;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.dto.UserDTO;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final BOUserRepository userRepository;

    public void save(User user, UserDTO userDTO) {
        for (Role role : userDTO.getRole()) {
            UserRole userRole = new UserRole(user, role);
            userRoleRepository.save(userRole);
        }
    }

    public UserRole createRoleAdminByEmail(@NotNull User user, @NotNull Role role) {
        UserRole userRole = userRoleRepository.save(new UserRole(user, role));
        user.getUserRoles().add(userRole);
        return userRole;
    }
}
