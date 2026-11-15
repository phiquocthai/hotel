package com.example.hotel_management_project.Config;

import com.example.hotel_management_project.Security.AuthEntryPoint;
import com.example.hotel_management_project.Security.AuthTokenFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class WebSecurityConfig {

    private final AuthEntryPoint authEntryPoint;
    private final AuthTokenFilter authTokenFilter;

    public WebSecurityConfig(AuthEntryPoint authEntryPoint, AuthTokenFilter authTokenFilter) {
        this.authEntryPoint = authEntryPoint;
        this.authTokenFilter = authTokenFilter;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new RbacAccessDeniedHandler();
    }

    static class RbacAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request,
                           HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Bạn không có quyền thực hiện hành động này.\",\"path\":\"" + request.getRequestURI() + "\"}");
        }
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // 1. Endpoint công khai (Public)
                .requestMatchers("/api/auth/**", "/auth/**", "/error").permitAll()
                .requestMatchers("/api/staff/images/**", "/uploads/**").permitAll()

                // 2. Phân quyền ADMIN (Quản lý loại phòng, Quản lý khuyến mãi, Quản lý phương thức thanh toán)
                .requestMatchers(HttpMethod.POST, "/api/room-type/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/room-type/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/room-type/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/promotions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/promotions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/promotions/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/payment-methods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payment-methods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/payment-methods/**").hasRole("ADMIN")

                // Xóa hóa đơn: chỉ Admin có quyền
                .requestMatchers(HttpMethod.DELETE, "/api/invoices/**").hasRole("ADMIN")

                // 3. Phân quyền MANAGER (Quản lý nhân viên & tài khoản, dịch vụ, ca làm việc, phân ca, chấm công) + Admin kế thừa
                .requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "MANAGER")

                .requestMatchers(HttpMethod.POST, "/api/services/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/services/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasAnyRole("ADMIN", "MANAGER")

                .requestMatchers("/api/work-shifts/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/shift-assignments/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "MANAGER")

                // Thêm / Xóa phòng vật lý: Manager & Admin
                .requestMatchers(HttpMethod.POST, "/api/rooms/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasAnyRole("ADMIN", "MANAGER")

                // 4. Phân quyền RECEPTIONIST (Lễ tân) + Manager & Admin kế thừa toàn bộ
                // Tra cứu danh mục để phục vụ nghiệp vụ (Loại phòng, Khuyến mãi, Phương thức TT, Dịch vụ)
                .requestMatchers(HttpMethod.GET, "/api/room-type/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers(HttpMethod.GET, "/api/promotions/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers(HttpMethod.GET, "/api/payment-methods/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers(HttpMethod.GET, "/api/services/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")

                // Nghiệp vụ Vận hành (Dashboard, Sơ đồ phòng, Đặt phòng, Check-in, Check-out, Khách hàng, DV phòng, Hóa đơn)
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers("/api/rooms/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers("/api/bookings/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers("/api/booking-service-details/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "MANAGER", "RECEPTION")

                // Tất cả các yêu cầu còn lại: Bắt buộc đã xác thực
                .anyRequest().authenticated()
        );
        http.sessionManagement(
                session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS)
        );

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler())
        );
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        http.csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}
