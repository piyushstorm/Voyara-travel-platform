package com.travelplatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPatternParsingResilience() {
        String messyInput = "\"https://voyara-8x1rxnz01-zuxxy2.vercel.app/\", 'http://localhost:5173/' ,  ";
        List<String> patterns = SecurityConfig.parseAllowedOriginPatterns(messyInput);

        assertTrue(patterns.contains("https://*.vercel.app"));
        assertTrue(patterns.contains("https://voyara*.vercel.app"));
        assertTrue(patterns.contains("https://voyara.com"));
        assertTrue(patterns.contains("http://localhost:*"));
        assertTrue(patterns.contains("https://voyara-8x1rxnz01-zuxxy2.vercel.app"));
        assertTrue(patterns.contains("http://localhost:5173"));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowCredentials(true);

        assertEquals("https://voyara-8x1rxnz01-zuxxy2.vercel.app",
                config.checkOrigin("https://voyara-8x1rxnz01-zuxxy2.vercel.app"));
        assertEquals("https://voyara.vercel.app",
                config.checkOrigin("https://voyara.vercel.app"));
        assertEquals("http://localhost:5173",
                config.checkOrigin("http://localhost:5173"));
    }

    @Test
    void testPreflightAndGetCORSForVercelPreview() throws Exception {
        String vercelOrigin = "https://voyara-8x1rxnz01-zuxxy2.vercel.app";

        // OPTIONS preflight for /api/holidays/search
        mockMvc.perform(options("/api/holidays/search")
                        .param("destination", "Kashmir")
                        .header("Origin", vercelOrigin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", vercelOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        // GET request with Origin
        mockMvc.perform(get("/api/holidays/search")
                        .param("destination", "Kashmir")
                        .header("Origin", vercelOrigin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", vercelOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        // OPTIONS preflight for /api/flights/search
        mockMvc.perform(options("/api/flights/search")
                        .header("Origin", vercelOrigin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", vercelOrigin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
