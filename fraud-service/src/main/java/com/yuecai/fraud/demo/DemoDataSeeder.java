package com.yuecai.fraud.demo;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.prediction.Prediction;
import com.yuecai.fraud.prediction.PredictionRepository;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a demo account and a few prediction records so a fresh environment has
 * something to log in to and look at. Only active under the {@code demo} profile
 * (docker compose, CI, on-demand demo deployments) — never in a default profile.
 *
 * <p>Idempotent: every item is created only if it does not exist, so restarts
 * keep whatever the demo user has done since.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String DEMO_USERNAME = "demo";
    /** Fixed so the seeded batch can be recognised (and skipped) on restart. */
    static final UUID DEMO_BATCH_ID = UUID.nameUUIDFromBytes("fraud-demo-batch".getBytes());

    private final UserRepository users;
    private final PredictionRepository predictions;
    private final PasswordEncoder encoder;
    private final String demoPassword;

    public DemoDataSeeder(UserRepository users, PredictionRepository predictions, PasswordEncoder encoder,
                          @Value("${demo.password:demo123}") String demoPassword) {
        this.users = users;
        this.predictions = predictions;
        this.encoder = encoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demo = users.findByUsername(DEMO_USERNAME).orElseGet(() -> {
            log.info("Seeding demo user '{}'", DEMO_USERNAME);
            return users.save(new User(DEMO_USERNAME, encoder.encode(demoPassword), "Demo", "User", "demo@example.com"));
        });

        if (predictions.existsByUserUsername(DEMO_USERNAME)) {
            log.info("Demo predictions already present; nothing to seed");
            return;
        }
        log.info("Seeding demo predictions for '{}'", DEMO_USERNAME);
        predictions.saveAll(List.of(
                Prediction.single(demo, PAYMENT_OK, SCORE_OK),
                Prediction.single(demo, CASHOUT_FRAUD, SCORE_FRAUD),
                Prediction.batchRow(demo, DEMO_BATCH_ID, 2, PAYMENT_OK, SCORE_OK),
                Prediction.batchRow(demo, DEMO_BATCH_ID, 3, TRANSFER_OK, SCORE_TRANSFER),
                Prediction.batchRow(demo, DEMO_BATCH_ID, 4, CASHOUT_FRAUD, SCORE_FRAUD)));
    }

    // Values below are real outputs of model_proto_rf.pkl (threshold 0.25) for these inputs,
    // recorded on 2026-09-15; they are fixtures, not live scores.
    private static final ModelFeatures PAYMENT_OK = ModelFeatures.of(1, 3, 9839.64, 170136.0, 160296.36, 0.0, 0.0);
    private static final ModelFeatures TRANSFER_OK = ModelFeatures.of(1, 4, 181.0, 181.0, 0.0, 0.0, 0.0);
    private static final ModelFeatures CASHOUT_FRAUD = ModelFeatures.of(100, 1, 10000.0, 10000.0, 0.0, 0.0, 10000.0);

    private static final ModelScore SCORE_OK = new ModelScore(0, 0.13, "No risk detected", "N/A", "N/A", 0.25);
    private static final ModelScore SCORE_TRANSFER = new ModelScore(0, 0.24, "No risk detected", "N/A", "N/A", 0.25);
    private static final ModelScore SCORE_FRAUD = new ModelScore(1, 0.39,
            "Receiver balance after transaction changed significantly",
            "High-risk transaction type (CASH_OUT)",
            "Receiver balance before transaction looks unusual", 0.25);
}
