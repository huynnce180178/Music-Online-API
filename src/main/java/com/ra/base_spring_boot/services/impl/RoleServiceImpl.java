package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService
{
    private final IRoleRepository roleRepository;
    @Autowired
    private final IUserRepository userRepository;

    @Override
    public Role findByRoleName(RoleName roleName)
    {
        return roleRepository.findByRoleName(roleName).orElseThrow(() -> new HttpNotFound("role not found"));
    }

    @Override
    public void save(Role role) {
        roleRepository.save(role);
    }
    @Override
    public Set<Role> getRolesUser(Long userID) {
        User user = userRepository.findById(userID).orElse(null);
        if(user == null){
            throw new IllegalArgumentException("user not found");
        }
        return user.getRoles();
    }

    @Override
    public Set<Role> getRolesUser( Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId).orElse(null);
        if(user == null){
            throw new IllegalArgumentException("user not found");
        }
        Set<Role> roles = user.getRoles();
        Set<Role> newRoles = new HashSet<>();
        for(Long roleId : roleIds){
            Role role = roleRepository.findById(roleId).orElse(null);
            newRoles.add(role);
        }
        if(!roles.stream().anyMatch(role -> role.getRoleName().equals("ROLE_USER"))){
            Role role = roleRepository.findByRoleName(RoleName.ROLE_USER).orElse(null);
            newRoles.add(role);
        }
        if(!user.getRoles().equals(newRoles)){
            user.setRoles(newRoles);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return user.getRoles();
    }
}
