package com.yuecai.fraud.prediction;

import static com.yuecai.fraud.TestFixtures.FRAUD;
import static com.yuecai.fraud.TestFixtures.FRAUD_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.modelclient.ModelServiceException;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock ModelServiceClient modelClient;
    @Mock PredictionRepository predictions;
    @Mock UserService users;
    @InjectMocks PredictionService service;

    private final User alice = new User("alice", "{noop}x", "Alice", "A", "a@x.io");

    @Test
    void scoresThroughModelAndPersistsResult() {
        when(users.requireByUsername("alice")).thenReturn(alice);
        when(modelClient.predict(any())).thenReturn(FRAUD_SCORE);
        when(predictions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PredictionResult result = service.predict("alice", FRAUD);

        assertThat(result.fraud()).isTrue();
        assertThat(result.probPercent()).isEqualTo(39);
        assertThat(result.type()).isEqualTo("CASH_OUT");
        assertThat(result.reasons()).containsExactly(
                "Receiver balance increased sharply",
                "High-risk transaction type (CASH_OUT)",
                "Sender balance dropped sharply");

        ArgumentCaptor<ModelFeatures> sent = ArgumentCaptor.forClass(ModelFeatures.class);
        verify(modelClient).predict(sent.capture());
        assertThat(sent.getValue().balanceDiffOrg()).isEqualTo(10000.0);
        assertThat(sent.getValue().balanceDiffDest()).isEqualTo(10000.0);

        ArgumentCaptor<Prediction> saved = ArgumentCaptor.forClass(Prediction.class);
        verify(predictions).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(Prediction.Source.SINGLE);
        assertThat(saved.getValue().getUser()).isSameAs(alice);
    }

    @Test
    void doesNotPersistWhenModelFails() {
        when(users.requireByUsername("alice")).thenReturn(alice);
        when(modelClient.predict(any())).thenThrow(new ModelServiceException("down"));

        assertThatThrownBy(() -> service.predict("alice", FRAUD))
                .isInstanceOf(ModelServiceException.class);
        verify(predictions, never()).save(any());
    }
}
