package com.example.tutorial.product;

import com.example.tutorial.TutorialApplication;
import com.example.tutorial.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TutorialApplication.class)
@AutoConfigureMockMvc
class ProductApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void rejectsUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsAndFetchesProductWithJwt() throws Exception {
        String token = jwtTokenService.createToken("demo");

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","price":32999.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String productId = response.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(32999.00));
    }

    @Test
    void validatesProductPayload() throws Exception {
        String token = jwtTokenService.createToken("demo");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","price":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.price").exists());
    }

    @Test
    void returns404WhenProductMissing() throws Exception {
        String token = jwtTokenService.createToken("demo");

        mockMvc.perform(get("/api/products/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
