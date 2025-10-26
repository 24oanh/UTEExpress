package ltweb.util;

import ltweb.entity.RoleType;
import ltweb.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Component
public class RoleChecker {
    
    public static boolean hasRole(User user, RoleType roleType) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleType));
    }
    
    public static boolean hasAnyRole(User user, RoleType... roleTypes) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        for (RoleType roleType : roleTypes) {
            if (hasRole(user, roleType)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasAllRoles(User user, RoleType... roleTypes) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        for (RoleType roleType : roleTypes) {
            if (!hasRole(user, roleType)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean isWarehouseStaff(User user) {
        return hasRole(user, RoleType.ROLE_WAREHOUSE_STAFF);
    }
    
    public static boolean isShipper(User user) {
        return hasRole(user, RoleType.ROLE_SHIPPER);
    }
    
    
    
    public static boolean currentUserHasRole(RoleType roleType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleType.name()));
    }
    
    public static boolean currentUserHasAnyRole(RoleType... roleTypes) {
        for (RoleType roleType : roleTypes) {
            if (currentUserHasRole(roleType)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean currentUserIsWarehouseStaff() {
        return currentUserHasRole(RoleType.ROLE_WAREHOUSE_STAFF);
    }
    
    public static boolean currentUserIsShipper() {
        return currentUserHasRole(RoleType.ROLE_SHIPPER);
    }
    
    
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities.isEmpty()) {
            return null;
        }
        
        return authorities.iterator().next().getAuthority();
    }
    
    public static Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }
}