package com.yuecai.fraud.web;

import com.yuecai.fraud.modelclient.ModelServiceException;
import com.yuecai.fraud.prediction.BatchPredictionService;
import com.yuecai.fraud.prediction.BatchResult;
import com.yuecai.fraud.prediction.EmptyBatchException;
import java.io.IOException;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** CSV upload page (batch.jsp). Replaces BatchServlet + BatchDownloadServlet. */
@Controller
public class BatchPageController {

    private static final int PREVIEW_ROWS = 10;

    private final BatchPredictionService batchService;

    public BatchPageController(BatchPredictionService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/batch")
    public String form(Model model) {
        model.addAttribute("activePage", "batch");
        return "batch";
    }

    @PostMapping("/batch")
    public String upload(@RequestParam("csvFile") MultipartFile csvFile, Principal principal, Model model)
            throws IOException {
        model.addAttribute("activePage", "batch");
        if (csvFile == null || csvFile.isEmpty()) {
            model.addAttribute("error", "Missing csvFile upload. Please select a CSV file.");
            return "batch";
        }
        try {
            BatchResult result = batchService.predictCsv(principal.getName(), csvFile.getInputStream());
            model.addAttribute("previewRows", result.preview(PREVIEW_ROWS));
            model.addAttribute("batchId", result.batchId());
            model.addAttribute("skipped", result.skipped());
            model.addAttribute("okMsg", "Batch prediction completed. Predicted transactions in total: "
                    + result.total() + " (flagged as fraud: " + result.fraudCount() + ").");
        } catch (EmptyBatchException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("skipped", e.getSkipped());
        } catch (IllegalArgumentException | ModelServiceException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "batch";
    }

    @GetMapping("/batch/{batchId}/download")
    public ResponseEntity<String> download(@PathVariable String batchId, Principal principal) {
        String csv = batchService.batchCsv(principal.getName(), batchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fd_batch_predictions.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
