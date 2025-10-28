package ltweb.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import ltweb.entity.User;
import ltweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component  // ✅ THÊM: Đảm bảo Spring quản lý bean này
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuthService authService;  // ✅ THÊM: Inject AuthService

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String username = authentication.getName();
        User user = authService.findByUsername(username);
        HttpSession session = request.getSession();
        session.setAttribute("currentUser", user);
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        String redirectUrl = determineTargetUrl(request, authorities);
        
        if (redirectUrl != null) {
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }
        
        // ✅ SỬA: Nếu không có role phù hợp, redirect về login với lỗi
        getRedirectStrategy().sendRedirect(request, response, "/login?error=true");
    }
    
    // ✅ THÊM: Method xác định URL redirect dựa trên role
    private String determineTargetUrl(HttpServletRequest request, Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            
            switch (role) {
                case "ROLE_ADMIN":
                    return "/admin/dashboard";
                case "ROLE_WAREHOUSE_STAFF":
                    return "/warehouse/dashboard";
                case "ROLE_SHIPPER":
                    return "/shipper/dashboard";
                case "ROLE_SUPPORT":
                    return "/support/chat";
                case "ROLE_CUSTOMER":
                    return "/customer/dashboard";
            }
        }
        return null;
    }
}