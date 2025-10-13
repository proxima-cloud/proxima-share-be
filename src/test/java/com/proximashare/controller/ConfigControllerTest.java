package com.proximashare.controller;

import com.proximashare.exception.GlobalExceptionHandler;
import com.proximashare.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@ContextConfiguration(classes = {ConfigController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.environment.production=false",
        "app.error.include-stacktrace=true"
})
@DisplayName("ConfigController Tests")
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleRepository roleRepository;

    @Nested
    @DisplayName("GET /api/public/config/roles")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should get all the roles in array")
        void shouldGetAllRoles() throws Exception {
            mockMvc.perform(get("/api/public/config/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
