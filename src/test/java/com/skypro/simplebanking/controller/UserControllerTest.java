package com.skypro.simplebanking.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.CreateUserRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import com.skypro.simplebanking.repository.UserRepository;

import java.util.ArrayList;
import java.util.Random;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ObjectMapper objectMapper;
    private final Faker faker = new Faker();

    @Test
    void createUserTest_WhenRoleIsAdmin_CorrectInputData_ThenReturnCorrectDTO() throws Exception {
        User user = generateUser();
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(post("/user/")
                        .with(user(bankingUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }

    @Test
    void createUserTest_WhenRoleIsUser_ThenReturn403Status() throws Exception {
        User user = generateUser();
        CreateUserRequest createUserRequest = getUserRequest(user);
        String content = objectMapper.writeValueAsString(createUserRequest);
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(post("/user/")
                        .with(user(bankingUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsersTest_WhenReturnCorrectListUsers() throws Exception {
        User user1 = generateUser();
        User user2 = generateUser();

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value(user1.getUsername()))
                .andExpect(jsonPath("$[0].id").value(user1.getId()))
                .andExpect(jsonPath("$[0].accounts").isNotEmpty())
                .andExpect(jsonPath("$[1].username").value(user2.getUsername()))
                .andExpect(jsonPath("$[1].id").value(user2.getId()))
                .andExpect(jsonPath("$[1].accounts").isNotEmpty());
    }

    @Test
    void getAllUsersTest_ThenReturn403Status() throws Exception {
        User user = userRepository.save(generateUser());
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(get("/user/list")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfileTest_WhenReturnCorrectUserDTO() throws Exception {
        User user = userRepository.save(generateUser());
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(get("/user/me")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }

    @Test
    void getMyProfileTest_WhenReturn403Status() throws Exception {
        User user = userRepository.save(generateUser());
        BankingUserDetails bankingUserDetails = getAdminBankingUserDetails(user);

        mockMvc.perform(get("/user/me")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isForbidden());
    }

    public User generateUser() {
        final String name = faker.name().username();
        final String password  = faker.number().digits(7);
        final long amount = 1L;
        User userTest = new User();
        userTest.setUsername(name);
        userTest.setPassword(password);

        userRepository.save(userTest);

        User user = userRepository.findByUsername(name).get();
        user.setAccounts(new ArrayList<>());

        Account accountTest = new Account();
        accountTest.setId(new Random().nextLong(1000));
        accountTest.setUser(userTest);
        accountTest.setAmount(amount);
        accountTest.setAccountCurrency(AccountCurrency.RUB);
        user.getAccounts().add(accountTest);

        accountRepository.save(accountTest);

        return user;
    }

    public CreateUserRequest getUserRequest(User user) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(user.getUsername());
        createUserRequest.setPassword(user.getPassword());
        return createUserRequest;
    }

    public BankingUserDetails getAdminBankingUserDetails(User user) {
        return new BankingUserDetails(new Random().nextLong(1000),
                user.getUsername(), user.getPassword(), true);
    }

    
}
