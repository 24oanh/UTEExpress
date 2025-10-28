// java/ltweb/service/UserService.java
package ltweb.service;

import ltweb.entity.RoleType;
import ltweb.entity.User;
import ltweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }

    public List<User> getUsersByRole(String roleType) {
        return userRepository.findByRolesName(RoleType.valueOf(roleType));
    }

    public List<User> getUsersByStatus(Boolean isActive) {
        return userRepository.findByIsActive(isActive);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public long countAllUsers() {
        return userRepository.count();
    }

    public long countUsersByRole(String roleType) {
        return userRepository.countByRolesName(RoleType.valueOf(roleType));
    }

    public long countActiveUsers() {
        return userRepository.countByIsActive(true);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}