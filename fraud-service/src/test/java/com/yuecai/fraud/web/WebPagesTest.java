package com.yuecai.fraud.web;

import static com.yuecai.fraud.TestFixtures.FRAUD_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The JSP pages themselves are not rendered by MockMvc (no Jasper); these tests cover the
 * controller + Spring Security behaviour that used to live in the servlets.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPagesTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PredictionRepository predictions;
    @Autowired com.yuecai.fraud.batch.BatchJobRepository batchJobs;
    @Autowired PasswordEncoder encoder;
    @MockitoBean ModelServiceClient modelClient;

    @BeforeEach
    void seedUser() {
        predictions.deleteAll();
        batchJobs.deleteAll();
        users.deleteAll();
        users.save(new User("alice", encoder.encode("secret"), "Alice", "A", "alice@example.com"));
    }

    @Test
    void protectedPagesRedirectToLogin() throws Exception {
        mvc.perform(get("/home")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/predict")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/batch")).andExpect(status().is3xxRedirection());
    }

    @Test
    void loginWithDatabaseUser() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername("alice"));

        mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void registerCreatesBcryptUserAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Bob").param("lastName", "B").param("email", "bob@example.com")
                        .param("username", "bob").param("password", "hunter22").param("password2", "hunter22"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User bob = users.findByUsername("bob").orElseThrow();
        assertThat(bob.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(encoder.matches("hunter22", bob.getPasswordHash())).isTrue();

        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Bob").param("lastName", "B").param("email", "bob@example.com")
                        .param("username", "bob").param("password", "hunter22").param("password2", "hunter22"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    void registerValidatesPasswordsMatch() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("firstName", "Bob").param("lastName", "B").param("email", "bob@example.com")
                        .param("username", "bob").param("password", "hunter22").param("password2", "nope"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "password2"));
        assertThat(users.findByUsername("bob")).isEmpty();
    }

    @Test
    void predictFormRendersResultAndRecordsIt() throws Exception {
        when(modelClient.predict(any())).thenReturn(FRAUD_SCORE);

        mvc.perform(post("/predict").with(csrf()).with(user("alice"))
                        .param("step", "100").param("typeCode", "1").param("amount", "10000")
                        .param("oldbalanceOrg", "10000").param("newbalanceOrig", "0")
                        .param("oldbalanceDest", "0").param("newbalanceDest", "10000"))
                .andExpect(status().isOk())
                .andExpect(view().name("predict"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("username", "alice"));

        assertThat(predictions.count()).isEqualTo(1);
    }

    @Test
    void predictFormShowsValidationErrorWithoutCallingModel() throws Exception {
        mvc.perform(post("/predict").with(csrf()).with(user("alice"))
                        .param("step", "100").param("typeCode", "7").param("amount", "10000")
                        .param("oldbalanceOrg", "10000").param("newbalanceOrig", "0")
                        .param("oldbalanceDest", "0").param("newbalanceDest", "10000"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeDoesNotExist("result"));
        assertThat(predictions.count()).isZero();
    }

    @Test
    void batchUploadCreatesJobAndStatusIsPollableWithTheSession() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "csvFile", "tx.csv", "text/csv", "1,3,9839.64,170136.0,160296.36,0.0,0.0\n".getBytes());

        String location = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/batch").file(file).with(csrf()).with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/batch/jobs/*"))
                .andReturn().getResponse().getRedirectedUrl();
        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(get("/batch/jobs/{id}", id).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("batch-job"))
                .andExpect(model().attributeExists("job"));

        mvc.perform(get("/batch/jobs/{id}/status", id).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("PENDING"));

        // someone else's job -> 404 page, not a leak
        mvc.perform(get("/batch/jobs/{id}", id).with(user("bob")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));

        // the same file again lands on the same job (idempotent)
        String again = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/batch").file(file).with(csrf()).with(user("alice")))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(again).isEqualTo(location);
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/predict").with(user("alice")).param("step", "1"))
                .andExpect(status().isForbidden());
    }
}
