package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.services.IRoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name="Role Controller")
@RestController
@RequestMapping("/api/v1")
@Validated
public class RoleController {
    @Autowired
    private IRoleService roleService;

    @GetMapping("/admin/role/getRole/{id}")
    public ResponseEntity<?> changeRole(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .data(roleService.getRolesUser(id))
                .code(200)
                .build());
    }
    @PatchMapping("/admin/role/setRole/{id}")
    public ResponseEntity<?> setRole(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .data(roleService.getRolesUser(id, roleIds))
                .code(200)
                .build());
    }
}
