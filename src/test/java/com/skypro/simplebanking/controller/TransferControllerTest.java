package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransferControllerTest {
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
    void transferTest_CorrectInputData_ThenTransferSuccessfully() throws Exception {
        User user1 = userRepository.save(generateUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user1);
        User user2 = userRepository.save(generateUser());
        long senderAmount = getUserAmount(user1, AccountCurrency.RUB);
        long receiverAmount = getUserAmount(user2, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(user1, AccountCurrency.RUB, user2, AccountCurrency.RUB, senderAmount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long senderActualAmount = getUserAmount(userRepository.findById(user1.getId()).orElseThrow(), AccountCurrency.RUB);
        long receiverActualAmount = getUserAmount(userRepository.findById(user2.getId()).orElseThrow(), AccountCurrency.RUB);

        assertThat(senderActualAmount).isEqualTo(0);
        assertThat(receiverActualAmount).isEqualTo(receiverAmount + senderAmount);
    }
    @Test
    void transferTest_WhenReturn404Status() throws Exception {
        User user1 = generateUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user1);
        User user2 = userRepository.save(generateUser());
        long senderAmount = getUserAmount(user1, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(user1, AccountCurrency.RUB, user2, AccountCurrency.RUB, senderAmount);
        String content = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
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

    public long getUserAmount(User user, AccountCurrency accountCurrency) {
        return user.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(accountCurrency))
                .mapToLong(Account::getAmount)
                .findAny()
                .orElseThrow();
    }

    public TransferRequest getTestTransferRequest(User fromUser, AccountCurrency fromAccountCurrency, User toUser, AccountCurrency toAccountCurrency, long amount) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(fromAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setToUserId(toUser.getId());
        transferRequest.setToAccountId(toUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(toAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setAmount(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(AccountCurrency.RUB))
                .findFirst()
                .map(x -> x.getAmount() - 1)
                .orElseThrow());
        transferRequest.setAmount(amount);
        return transferRequest;
    }
}
