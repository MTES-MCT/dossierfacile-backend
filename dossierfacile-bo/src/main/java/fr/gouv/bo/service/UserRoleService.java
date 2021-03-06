package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.entity.UserRole;
import fr.dossierfacile.common.enums.Role;
import fr.gouv.bo.dto.UserDTO;
import fr.gouv.bo.repository.UserRepository;
import fr.gouv.bo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public void save(User user, UserDTO userDTO) {
        for (Role role : userDTO.getRole()) {
            UserRole userRole = new UserRole(user, role);
            userRoleRepository.save(userRole);
        }
    }

    public void createRoleAdminByEmail(String userEmail, User user, String create_user) {
        UserRole newUserRole;
        if(user == null){
            user = userRepository.findOneByEmail(userEmail);
        }
        if(create_user.equals("create_admin")){
            newUserRole = new UserRole(user, Role.ROLE_ADMIN);
        } else{
            newUserRole = new UserRole(user, Role.ROLE_OPERATOR);
        }

        userRoleRepository.save(newUserRole);
    }
}
