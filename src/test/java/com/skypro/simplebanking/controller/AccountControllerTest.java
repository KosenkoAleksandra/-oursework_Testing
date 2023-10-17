package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import com.github.javafaker.Faker;

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
public class AccountControllerTest {
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
    void getUserAccountTest_CorrectInputData_ThenReturnCorrectAccounts() throws Exception {
        User user = userRepository.save(generateUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        Account account = user.getAccounts().stream().findFirst().get();


        mockMvc.perform(get("/account/{id}", account.getId())
                            .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.amount").value(account.getAmount()))
                .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().toString()));

    }
    @Test
    void getUserAccountTest_WhenReturn404Status() throws Exception{
        User user = generateUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        Account account = user.getAccounts().stream().findFirst().get();

        mockMvc.perform(get("/account/{id}", account.getId())
                            .with(user(userDetails)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER", password = "user")
    void depositToAccountTest_CorrectInputData_ThenReturnCorrectAccount() throws Exception{
        User user = generateUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        Account account = user.getAccounts().stream().findFirst().get();

        long amountChange = 10L;
        long amountExpected = account.getAmount() + amountChange;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.amount").value(amountExpected))
                .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().toString()));
        }

    @Test
    @WithMockUser(username = "user", roles = "USER", password = "user")
    void withdrawFromAccountTest_CorrectInputData_ThenReturnCorrectAccount() throws Exception{
        User user = generateUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        Account account = user.getAccounts().stream().findFirst().get();

        long amountChange = account.getAmount();
        long amountExpected = account.getAmount() - amountChange;

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String content = objectMapper.writeValueAsString(balanceChangeRequest);


        mockMvc.perform(post("/account/deposit/{id}", account.getId())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.amount").value(amountExpected))
                .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().toString()));
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
}
