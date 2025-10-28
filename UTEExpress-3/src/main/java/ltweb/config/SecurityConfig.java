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
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.sendRedirect("/login?logout=true");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/forgot-password", "/reset-password").permitAll()
                .requestMatchers("/customer/register", "/customer/verify",
                        "/customer/forgot-password", "/customer/reset-password",
                        "/customer/resend-verification", "/customer/notifications/**")
                .permitAll()
                .requestMatchers("/customer/payment/callback", "/api/payment/**").permitAll()
                .requestMatchers("/shipper/shipments/**", "/shipper/location/**").hasRole("SHIPPER")
                .requestMatchers("/shipper/**").hasRole("SHIPPER")
                .requestMatchers("/customer/payment/**").hasRole("CUSTOMER")
                .requestMatchers("/customer/**", "/customer/orders/create", "/customer/orders/calculate",
                        "/customer/chat/**")
                .hasRole("CUSTOMER")
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
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied"))
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true"))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**", "/shipper/location/**", "/ws/**"));

        return http.build();
    }
}