package com.yuecai.fraud.prediction;

import com.yuecai.fraud.modelclient.ModelFeatures;
import com.yuecai.fraud.modelclient.ModelScore;
import com.yuecai.fraud.modelclient.ModelServiceClient;
import com.yuecai.fraud.user.User;
import com.yuecai.fraud.user.UserService;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch scoring. Unlike the legacy servlet, which issued one SOAP call per CSV row and wrote a
 * temp file per upload, this sends the whole file to the model's batch endpoint (in chunks) and
 * keeps the results in the database keyed by a batch id.
 */
@Service
public class BatchPredictionService {

    public static final int MAX_ROWS = 10_000;

    private final CsvTransactionParser parser;
    private final ModelServiceClient modelClient;
    private final PredictionRepository predictions;
    private final UserService users;

    public BatchPredictionService(CsvTransactionParser parser, ModelServiceClient modelClient,
                                  PredictionRepository predictions, UserService users) {
        this.parser = parser;
        this.modelClient = modelClient;
        this.predictions = predictions;
        this.users = users;
    }

    @Transactional
    public BatchResult predictCsv(String username, InputStream csv) {
        User user = users.requireByUsername(username);
        CsvTransactionParser.ParseResult parsed = parser.parse(csv);
        if (parsed.rows().isEmpty()) {
            throw new EmptyBatchException(parsed.skipped());
        }
        if (parsed.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("CSV has " + parsed.rows().size()
                    + " rows; the limit is " + MAX_ROWS);
        }

        List<ModelFeatures> features = parsed.rows().stream()
                .map(r -> r.transaction().toFeatures())
                .toList();
        List<ModelScore> scores = modelClient.predictBatch(features);

        UUID batchId = UUID.randomUUID();
        List<Prediction> rows = new ArrayList<>(features.size());
        for (int i = 0; i < features.size(); i++) {
            rows.add(Prediction.batchRow(user, batchId, parsed.rows().get(i).lineNumber(),
                    features.get(i), scores.get(i)));
        }
        List<PredictionResult> results = predictions.saveAll(rows).stream()
                .map(PredictionResult::from)
                .toList();
        long fraudCount = results.stream().filter(PredictionResult::fraud).count();
        return new BatchResult(batchId.toString(), results.size(), fraudCount, parsed.skipped(), results);
    }

    @Transactional(readOnly = true)
    public List<PredictionResult> batchRows(String username, String batchId) {
        List<Prediction> rows = predictions.findByBatchIdAndUserUsernameOrderByCsvRowAsc(batchId, username);
        if (rows.isEmpty()) {
            throw new BatchNotFoundException(batchId);
        }
        return rows.stream().map(PredictionResult::from).toList();
    }

    /** Renders a batch as CSV for download. */
    @Transactional(readOnly = true)
    public String batchCsv(String username, String batchId) {
        return PredictionCsv.write(batchRows(username, batchId));
    }
}
