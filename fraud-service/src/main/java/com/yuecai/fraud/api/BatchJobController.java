package com.yuecai.fraud.api;

import com.yuecai.fraud.batch.BatchJob;
import com.yuecai.fraud.batch.BatchJobResponse;
import com.yuecai.fraud.batch.BatchJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Asynchronous batch scoring: submit a CSV, poll the job, download the result.
 * (The synchronous {@code POST /api/v1/predictions/batch} remains for small files.)
 */
@RestController
@RequestMapping(path = "/api/v1/batch-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Batch jobs", description = "Upload a CSV, get a job id back, poll for progress, download the result")
public class BatchJobController {

    private final BatchJobService service;

    public BatchJobController(BatchJobService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a CSV for asynchronous scoring",
            description = "Returns 202 with a Location header. Send an Idempotency-Key header to make retries safe; "
                    + "without one, the file's SHA-256 is used, so re-uploading the same file returns the same job (200).")
    @ApiResponse(responseCode = "202", description = "Job created")
    @ApiResponse(responseCode = "200", description = "An identical submission already exists; that job is returned")
    public ResponseEntity<BatchJobResponse> submit(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Client-chosen key that makes the submit idempotent")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal) throws IOException {
        BatchJobService.Submission s = service.submit(principal.getName(), file.getBytes(),
                file.getOriginalFilename(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(s.job().getId()).toUri();
        return ResponseEntity.status(s.created() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .location(location)
                .body(toResponse(s.job()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Job status and progress")
    public BatchJobResponse get(@PathVariable String id, Principal principal) {
        return toResponse(service.get(principal.getName(), id));
    }

    @GetMapping
    @Operation(summary = "My jobs, newest first")
    public Page<BatchJobResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Principal principal) {
        return service.list(principal.getName(), PageRequest.of(page, Math.min(size, 100))).map(this::toResponse);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Re-queue a FAILED job (resumes from its checkpoint)")
    public BatchJobResponse retry(@PathVariable String id, Principal principal) {
        return toResponse(service.retry(principal.getName(), id));
    }

    @GetMapping(path = "/{id}/result", produces = "text/csv")
    @Operation(summary = "Download the result CSV",
            description = "302 to a time-limited S3 URL when object storage supports it; otherwise the CSV itself.")
    public ResponseEntity<byte[]> result(@PathVariable String id, Principal principal) {
        Optional<URI> url = service.resultUrl(principal.getName(), id);
        if (url.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(url.get()).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fraud-batch-" + id + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(service.resultCsv(principal.getName(), id));
    }

    private BatchJobResponse toResponse(BatchJob job) {
        String resultUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/batch-jobs/{id}/result").buildAndExpand(job.getId()).toUriString();
        return BatchJobResponse.from(job, resultUrl);
    }
}
