package com.yuecai.fraud.modelclient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelFeaturesTest {

    @Test
    void derivesBalanceDiffsLikeTheLegacySoapClient() {
        ModelFeatures f = ModelFeatures.of(1, 4, 181.0, 181.0, 0.0, 100.0, 281.0);

        assertThat(f.balanceDiffOrg()).isEqualTo(181.0);   // old - new (sender)
        assertThat(f.balanceDiffDest()).isEqualTo(181.0);  // new - old (receiver)
    }
}
