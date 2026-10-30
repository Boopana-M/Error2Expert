package com.codememory.controller;

import com.codememory.config.JwtUtil;
import com.codememory.dto.SubmissionRequest;
import com.codememory.entity.User;
import com.codememory.entity.enums.SubmissionResult;
import com.codememory.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    void setUp() {
        User user = userRepository.findByEmail("submissiontest@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Submission Tester")
                        .email("submissiontest@example.com")
                        .passwordHash("hashed")
                        .build()));

        token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());
    }

    @Test
    @DisplayName("POST /api/submissions - successfully records failed attempt and classifies boundary error")
    void testCreateSubmissionWithMistake() throws Exception {
        SubmissionRequest request = SubmissionRequest.builder()
                .problemTitle("Binary Search")
                .difficulty("EASY")
                .topics(List.of("Binary Search", "Array"))
                .code("class Solution { public int search(int[] nums, int target) { ... } }")
                .language("java")
                .result(SubmissionResult.WRONG_ANSWER)
                .userExplanation("I set high = mid instead of high = mid - 1 causing boundary failure")
                .build();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.problemTitle", is("Binary Search")))
                .andExpect(jsonPath("$.data.result", is("WRONG_ANSWER")))
                .andExpect(jsonPath("$.data.mistake", notNullValue()))
                .andExpect(jsonPath("$.data.mistake.category", is("BOUNDARY_ERROR")));
    }

    @Test
    @DisplayName("POST /api/submissions - fails validation when problem title is blank")
    void testCreateSubmissionValidationFailure() throws Exception {
        SubmissionRequest request = SubmissionRequest.builder()
                .problemTitle("")
                .result(SubmissionResult.WRONG_ANSWER)
                .build();

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.problemTitle", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/submissions - retrieves all recorded submissions for authenticated user")
    void testGetAllSubmissions() throws Exception {
        mockMvc.perform(get("/api/submissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(List.class)));
    }
}
