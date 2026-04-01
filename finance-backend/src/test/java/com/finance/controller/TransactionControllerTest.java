package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance.config.ApplicationConfig;
import com.finance.config.SecurityConfig;
import com.finance.dto.TransactionRequest;
import com.finance.dto.TransactionResponse;
import com.finance.model.TransactionType;
import com.finance.repository.UserRepository;
import com.finance.service.JwtService;
import com.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, ApplicationConfig.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TransactionService transactionService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private TransactionResponse mockResponse;
    private TransactionRequest mockRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockResponse = TransactionResponse.builder()
                .id(1L)
                .title("Rent")
                .amount(BigDecimal.valueOf(1500))
                .type(TransactionType.DEBIT)
                .category("Housing")
                .date(LocalDate.of(2024, 1, 1))
                .description("Monthly rent")
                .ownerEmail("john@example.com")
                .ownerName("John Doe")
                .build();

        mockRequest = new TransactionRequest();
        mockRequest.setTitle("Rent");
        mockRequest.setAmount(BigDecimal.valueOf(1500));
        mockRequest.setType(TransactionType.DEBIT);
        mockRequest.setCategory("Housing");
        mockRequest.setDate(LocalDate.of(2024, 1, 1));
        mockRequest.setDescription("Monthly rent");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/transactions - Admin can create transaction")
    void createTransaction_adminSuccess() throws Exception {
        when(transactionService.create(any(TransactionRequest.class), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Rent"))
                .andExpect(jsonPath("$.amount").value(1500))
                .andExpect(jsonPath("$.category").value("Housing"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/transactions - User is forbidden from creating")
    void createTransaction_userForbidden() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "ANALYST")
    @DisplayName("GET /api/transactions - Analyst can get paginated list")
    void getAllTransactions_userSuccess() throws Exception {
        Page<TransactionResponse> page = new PageImpl<>(List.of(mockResponse));
        when(transactionService.getAll(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Rent"));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "ANALYST")
    @DisplayName("GET /api/transactions/{id} - Analyst can get their transaction")
    void getTransactionById_success() throws Exception {
        when(transactionService.getById(eq(1L), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.ownerEmail").value("john@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/transactions/{id} - Admin can delete")
    void deleteTransaction_adminSuccess() throws Exception {
        mockMvc.perform(delete("/api/transactions/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/transactions/{id} - User is forbidden")
    void deleteTransaction_userForbidden() throws Exception {
        mockMvc.perform(delete("/api/transactions/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/transactions - Unauthenticated returns 401")
    void getAllTransactions_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
