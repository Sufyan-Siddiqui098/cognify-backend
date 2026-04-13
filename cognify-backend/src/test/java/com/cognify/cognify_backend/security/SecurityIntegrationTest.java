package com.cognify.cognify_backend.security;

import com.cognify.cognify_backend.config.SecurityConfig;
import com.cognify.cognify_backend.controller.AdminController;
import com.cognify.cognify_backend.controller.DocumentController;
import com.cognify.cognify_backend.service.AdminService;
import com.cognify.cognify_backend.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DocumentController.class, AdminController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityIntegrationTest.TestCorsConfig.class})
class SecurityIntegrationTest {

    @TestConfiguration
    static class TestCorsConfig {
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.addAllowedOrigin("http://localhost:3000");
            configuration.addAllowedMethod("*");
            configuration.addAllowedHeader("*");
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void accessProtectedEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/documents").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessAdminEndpointsReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
