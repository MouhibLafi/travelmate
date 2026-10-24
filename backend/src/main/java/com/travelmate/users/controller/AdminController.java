package com.travelmate.users.controller;

import com.travelmate.users.dto.UserResponse;
import com.travelmate.users.entity.Role;
import com.travelmate.users.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

// Reserve aux administrateurs (voir SecurityConfig : /api/admin/** -> hasRole("ADMIN"))
@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/admin/users?search=karim
    @GetMapping
    public List<UserResponse> findAll(@RequestParam(required = false) String search) {
        return userService.findAll(search);
    }

    // PUT /api/admin/users/5/role   body : { "role": "ADMIN" }
    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @RequestBody Map<String, Role> body, Principal principal) {
        return userService.changeRole(id, body.get("role"), principal.getName());
    }

    // PUT /api/admin/users/5/status   body : { "active": false }
    @PutMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body, Principal principal) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return userService.changeStatus(id, active, principal.getName());
    }

    // DELETE /api/admin/users/5
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Principal principal) {
        userService.deleteUser(id, principal.getName());
    }
}
