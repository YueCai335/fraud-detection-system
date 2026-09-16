package com.yuecai.fraud.web;

import com.yuecai.fraud.batch.BatchJob;
import com.yuecai.fraud.batch.BatchJobResponse;
import com.yuecai.fraud.batch.BatchJobService;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Batch pages: upload a CSV (batch.jsp), then watch the background job (batch-job.jsp).
 * The status endpoint is served here, under the session-based security chain, so the page's
 * JavaScript can poll it with the login cookie.
 */
@Controller
public class BatchPageController {

    private static final int PREVIEW_ROWS = 10;

    private final BatchJobService jobs;

    public BatchPageController(BatchJobService jobs) {
        this.jobs = jobs;
    }

    @GetMapping("/batch")
    public String page(Principal principal, Model model) {
        model.addAttribute("activePage", "batch");
        model.addAttribute("jobs", jobs.list(principal.getName(), PageRequest.of(0, 20))
                .map(j -> BatchJobResponse.from(j, null)).getContent());
        return "batch";
    }

    @PostMapping("/batch")
    public String upload(@RequestParam("csvFile") MultipartFile csvFile, Principal principal,
                         Model model, RedirectAttributes redirect) throws IOException {
        if (csvFile == null || csvFile.isEmpty()) {
            model.addAttribute("activePage", "batch");
            model.addAttribute("error", "Missing csvFile upload. Please select a CSV file.");
            model.addAttribute("jobs", jobs.list(principal.getName(), PageRequest.of(0, 20))
                    .map(j -> BatchJobResponse.from(j, null)).getContent());
            return "batch";
        }
        BatchJobService.Submission s = jobs.submit(principal.getName(), csvFile.getBytes(),
                csvFile.getOriginalFilename(), null);
        return "redirect:/batch/jobs/" + s.job().getId();
    }

    @GetMapping("/batch/jobs/{id}")
    public String job(@PathVariable String id, Principal principal, Model model) {
        BatchJob job = jobs.get(principal.getName(), id);
        model.addAttribute("activePage", "batch");
        model.addAttribute("job", BatchJobResponse.from(job, null));
        if (job.getStatus() == BatchJob.Status.SUCCEEDED) {
            model.addAttribute("previewRows", jobs.results(principal.getName(), id, PREVIEW_ROWS));
        }
        return "batch-job";
    }

    /** Polled by batch-job.jsp. */
    @GetMapping(path = "/batch/jobs/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BatchJobResponse status(@PathVariable String id, Principal principal) {
        return BatchJobResponse.from(jobs.get(principal.getName(), id), null);
    }

    @PostMapping("/batch/jobs/{id}/retry")
    public String retry(@PathVariable String id, Principal principal) {
        jobs.retry(principal.getName(), id);
        return "redirect:/batch/jobs/" + id;
    }

    @GetMapping("/batch/jobs/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id, Principal principal) {
        Optional<URI> url = jobs.resultUrl(principal.getName(), id);
        if (url.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(url.get()).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fd_batch_predictions.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(jobs.resultCsv(principal.getName(), id));
    }
}
