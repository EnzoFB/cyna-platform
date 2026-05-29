package com.cyna.shared.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCsrfGuardFilterTest {

    private final AuthCsrfGuardFilter filter = new AuthCsrfGuardFilter(
            new ObjectMapper().findAndRegisterModules(),
            java.util.List.of("http://localhost:4200", "http://localhost:4201")
    );

    @Test
    void should_block_cross_site_origin_on_auth_post() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passChain(204));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
    }

    @Test
    void should_allow_configured_frontend_origin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("Origin", "http://localhost:4200");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passChain(204));

        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void should_block_cross_site_fetch_metadata() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        request.addHeader("Sec-Fetch-Site", "cross-site");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passChain(204));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
    }

    @Test
    void should_allow_unsafe_auth_request_without_origin_headers_for_non_browser_clients() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passChain(204));

        assertThat(response.getStatus()).isEqualTo(204);
    }

    @Test
    void should_not_apply_to_non_auth_paths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/account/profile");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, passChain(204));

        assertThat(response.getStatus()).isEqualTo(204);
    }

    private static FilterChain passChain(int status) {
        return (req, res) -> ((MockHttpServletResponse) res).setStatus(status);
    }
}
