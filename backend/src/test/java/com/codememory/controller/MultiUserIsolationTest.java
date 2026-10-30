package com.codememory.controller;

import com.codememory.dto.SubmissionRequest;
import com.codememory.dto.auth.LoginRequest;
import com.codememory.dto.auth.RegisterRequest;
import com.codememory.entity.enums.SubmissionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiUserIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String name, String email, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(name, email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    private Long getUserIdFromToken(String token) throws Exception {
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(meResult.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    @Test
    @DisplayName("Verify complete multi-user isolation across all 5 Section 16 requirements")
    void testCompleteMultiUserLifecycleAndStrictIsolation() throws Exception {
        // =========================================================================
        // Test 1: Register User A -> Starts completely fresh (0 records)
        // =========================================================================
        String tokenA = registerAndGetToken("Alice", "alice@testisolation.com", "Password123!");
        Long userAId = getUserIdFromToken(tokenA);
        assertThat(userAId).isNotNull();

        // Verify User A summary is strictly zero
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemsAttempted", is(0)))
                .andExpect(jsonPath("$.problemsAccepted", is(0)))
                .andExpect(jsonPath("$.totalMistakes", is(0)))
                .andExpect(jsonPath("$.recurringMistakes", is(0)))
                .andExpect(jsonPath("$.currentMastery", is(0)))
                .andExpect(jsonPath("$.weakestConcept", is("—")));

        // Verify User A weaknesses is empty list
        mockMvc.perform(get("/api/weaknesses")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify User A recommendations is empty list
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Verify User A trends is empty list
        mockMvc.perform(get("/api/dashboard/trends")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // =========================================================================
        // Test 2: User A creates activity (1 real submission with boundary error)
        // =========================================================================
        SubmissionRequest submissionAlice = SubmissionRequest.builder()
                .problemTitle("Binary Search")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .code("class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length; while(l<r) { int m=(l+r)/2; l=m; } return -1; } }")
                .language("java")
                .result(SubmissionResult.WRONG_ANSWER)
                .attemptNumber(1)
                .userExplanation("I set low = mid causing infinite loop on boundary condition")
                .build();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submissionAlice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.mistake.category", is("BOUNDARY_ERROR")));

        // Verify User A summary updated to 1 problem, 1 mistake
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemsAttempted", is(1)))
                .andExpect(jsonPath("$.totalMistakes", is(1)));

        // =========================================================================
        // Test 3: Register User B -> Starts completely fresh (0 records)
        // Must NOT see User A's activity
        // =========================================================================
        String tokenB = registerAndGetToken("Bob", "bob@testisolation.com", "Password456!");
        Long userBId = getUserIdFromToken(tokenB);
        assertThat(userBId).isNotEqualTo(userAId);

        // Verify User B summary has ZERO attempts, ZERO mistakes
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemsAttempted", is(0)))
                .andExpect(jsonPath("$.totalMistakes", is(0)))
                .andExpect(jsonPath("$.currentMastery", is(0)));

        // Verify User B submissions is empty
        mockMvc.perform(get("/api/submissions")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Verify User B weaknesses is empty
        mockMvc.perform(get("/api/weaknesses")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // =========================================================================
        // Test 4: Switch users -> Login as A, verify A's data; Login as B, verify B's
        // =========================================================================
        // User A login
        LoginRequest loginReqA = new LoginRequest("alice@testisolation.com", "Password123!");
        MvcResult loginResA = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqA)))
                .andExpect(status().isOk())
                .andReturn();
        String reloginTokenA = objectMapper.readTree(loginResA.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // User A still has their 1 submission
        mockMvc.perform(get("/api/submissions")
                        .header("Authorization", "Bearer " + reloginTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].problemTitle", is("Binary Search")));

        // User B login
        LoginRequest loginReqB = new LoginRequest("bob@testisolation.com", "Password456!");
        MvcResult loginResB = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqB)))
                .andExpect(status().isOk())
                .andReturn();
        String reloginTokenB = objectMapper.readTree(loginResB.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // User B still has 0 submissions
        mockMvc.perform(get("/api/submissions")
                        .header("Authorization", "Bearer " + reloginTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // =========================================================================
        // Test 5: API Manipulation -> User B attempts spoofing ?userId=<User A ID>
        // =========================================================================
        // When User B calls /api/dashboard/summary?userId=<userAId>, it MUST ignore spoofed userId and return B's (0)
        mockMvc.perform(get("/api/dashboard/summary?userId=" + userAId)
                        .header("Authorization", "Bearer " + reloginTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemsAttempted", is(0)))
                .andExpect(jsonPath("$.totalMistakes", is(0)));

        // When User B calls /api/submissions?userId=<userAId>, it MUST return B's (0)
        mockMvc.perform(get("/api/submissions?userId=" + userAId)
                        .header("Authorization", "Bearer " + reloginTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // When User B tries to view User A's profile via /api/users/{userAId}, it is blocked with 401 Unauthorized
        mockMvc.perform(get("/api/users/" + userAId)
                        .header("Authorization", "Bearer " + reloginTokenB))
                .andExpect(status().isUnauthorized());
    }
}
