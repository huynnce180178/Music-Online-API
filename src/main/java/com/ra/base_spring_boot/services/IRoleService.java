package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.constants.RoleName;

import java.util.List;
import java.util.Set;

public interface IRoleService
{
    Role findByRoleName(RoleName roleName);
    void save(Role role);
    Set<Role> getRolesUser(Long userId);
    Set<Role> getRolesUser(Long userId, List<Long> roleIds);
}
