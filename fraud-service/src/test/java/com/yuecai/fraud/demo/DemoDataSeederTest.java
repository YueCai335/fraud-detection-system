package com.yuecai.fraud.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The demo account must exist only when the {@code demo} profile is on. Both halves are
 * checked here so the promise "no default credentials outside demo" is enforced by CI.
 */
class DemoDataSeederTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class WithoutDemoProfile {

        @Autowired UserRepository users;

        @Test
        void freshDatabaseHasNoDemoUser() {
            assertThat(users.findByUsername(DemoDataSeeder.DEMO_USERNAME)).isEmpty();
        }
    }

    @Nested
    // Own H2 database: the in-memory DB is shared per JVM, so seeding here must not leak
    // into the context above.
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fraud_demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")
    @ActiveProfiles({"test", "demo"})
    class WithDemoProfile {

        @Autowired UserRepository users;
        @Autowired PredictionRepository predictions;
        @Autowired PasswordEncoder encoder;
        @Autowired DemoDataSeeder seeder;
        @MockitoBean ModelServiceClient modelClient;

        @Test
        void seedsDemoUserAndPredictionsOnce() {
            User demo = users.findByUsername("demo").orElseThrow();
            assertThat(encoder.matches("demo123", demo.getPasswordHash())).isTrue();
            long before = predictions.count();
            assertThat(before).isEqualTo(5);

            seeder.run(null);  // a restart must not duplicate anything

            assertThat(users.findAll()).filteredOn(u -> u.getUsername().equals("demo")).hasSize(1);
            assertThat(predictions.count()).isEqualTo(before);
            assertThat(predictions.countByBatchIdAndFraudTrue(DemoDataSeeder.DEMO_BATCH_ID.toString())).isEqualTo(1);
        }
    }
}
