package ltweb.config;

import ltweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            .requestMatchers("/customer/register", "/customer/verify", "/customer/forgot-password",
                "/customer/reset-password", "/customer/resend-verification",
                "/customer/notifications/**").permitAll()
            
            // ✅ SỬA: Thêm callback và API paths
            .requestMatchers("/customer/payment/callback", "/api/payment/**").permitAll()
            
            // ✅ SỬA: Thêm shipper paths
            .requestMatchers("/shipper/shipments/**", "/shipper/location/**").hasRole("SHIPPER")
            .requestMatchers("/shipper/**").hasRole("SHIPPER")
            
            .requestMatchers("/customer/payment/**").hasRole("CUSTOMER")
            .requestMatchers("/customer/**", "/customer/orders/create", "/customer/orders/calculate",
                "/customer/chat/**").hasRole("CUSTOMER")
            .requestMatchers("/warehouse/**").hasRole("WAREHOUSE_STAFF")
            .requestMatchers("/admin/**", "/admin/chat/**", "/admin/notifications/**").hasRole("ADMIN")
            .requestMatchers("/support/**").hasRole("SUPPORT")
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(customAuthenticationSuccessHandler)
            .failureHandler((request, response, exception) -> {
                request.getSession().setAttribute("loginError", "Invalid username or password");
                response.sendRedirect("/login?error");
            })
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout=true")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll())
        .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
        .sessionManagement(session -> session.maximumSessions(1).maxSessionsPreventsLogin(false))
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**", "/shipper/location/**", "/ws/**") // ✅ THÊM paths ignore CSRF
        );
        
        return http.build();
    }
}