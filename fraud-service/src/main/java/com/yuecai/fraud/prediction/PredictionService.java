package com.yuecai.fraud.prediction;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scores a single transaction through the model service and records the result for the user. */
@Service
public class PredictionService {

    private final ModelServiceClient modelClient;
    private final PredictionRepository predictions;
    private final UserService users;

    public PredictionService(ModelServiceClient modelClient, PredictionRepository predictions, UserService users) {
        this.modelClient = modelClient;
        this.predictions = predictions;
        this.users = users;
    }

    @Transactional
    public PredictionResult predict(String username, TransactionRequest request) {
        User user = users.requireByUsername(username);
        ModelFeatures features = request.toFeatures();
        ModelScore score = modelClient.predict(features);
        Prediction saved = predictions.save(Prediction.single(user, features, score));
        return PredictionResult.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PredictionResult> history(String username, Pageable pageable) {
        return predictions.findByUserUsernameOrderByCreatedAtDesc(username, pageable)
                .map(PredictionResult::from);
    }
}
