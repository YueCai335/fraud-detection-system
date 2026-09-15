package com.yuecai.fraud.api;

import com.yuecai.fraud.prediction.BatchPredictionService;
import com.yuecai.fraud.prediction.BatchResult;
import com.yuecai.fraud.prediction.PredictionResult;
import com.yuecai.fraud.prediction.PredictionService;
import com.yuecai.fraud.prediction.TransactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/v1/predictions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Predictions", description = "Score transactions and browse past results")
public class PredictionController {

    private final PredictionService predictionService;
    private final BatchPredictionService batchService;

    public PredictionController(PredictionService predictionService, BatchPredictionService batchService) {
        this.predictionService = predictionService;
        this.batchService = batchService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Score one transaction")
    @ApiResponse(responseCode = "200", description = "Scored and stored")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "503", description = "Model service unavailable")
    public PredictionResult predict(@Valid @RequestBody TransactionRequest request, Principal principal) {
        return predictionService.predict(principal.getName(), request);
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Score a CSV of transactions",
            description = "Columns: step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest "
                    + "(header row optional, max 10,000 rows).")
    public BatchResult predictBatch(@RequestParam("file") MultipartFile file, Principal principal) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return batchService.predictCsv(principal.getName(), file.getInputStream());
    }

    @GetMapping("/batches/{batchId}/csv")
    @Operation(summary = "Download a previous batch as CSV")
    public ResponseEntity<String> batchCsv(@PathVariable String batchId, Principal principal) {
        String csv = batchService.batchCsv(principal.getName(), batchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fraud-batch-" + batchId + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping
    @Operation(summary = "List the caller's past predictions, newest first")
    public Page<PredictionResult> history(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          Principal principal) {
        return predictionService.history(principal.getName(), PageRequest.of(page, Math.min(size, 200)));
    }
}
