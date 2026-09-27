package com.example.vitals.controller;

import com.example.vitals.dto.CreateSessionRequest;
import com.example.vitals.dto.VerifySessionRequest;
import com.example.vitals.entity.VitalsSession;
import com.example.vitals.pdf.PdfReportGenerator;
import com.example.vitals.service.VitalsSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/vitals/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VitalsController {

    private final VitalsSessionService service;
    private final PdfReportGenerator pdfReportGenerator;

    /**
     * Called by the frontend / AI worker right after a webcam scan finishes
     * producing an rPPG-based heart rate + respiration rate estimate.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request) {
        try {
            VitalsSession session = service.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Called when a booth volunteer keys in the ground-truth BPM read off a
     * physical finger pulse oximeter, to benchmark the AI estimate.
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifySession(@PathVariable Long id, @RequestBody VerifySessionRequest request) {
        try {
            VitalsSession session = service.verifySession(id, request);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns every run recorded so far, for the live analytics/benchmark table.
     */
    @GetMapping("/all")
    public ResponseEntity<List<VitalsSession>> getAllSessions() {
        return ResponseEntity.ok(service.getAllSessions());
    }

    /**
     * Streams a clinical-style PDF screening report for one session.
     * Opens directly in-browser (inline) so it also works as a live demo.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getSessionPdf(@PathVariable Long id) {
        VitalsSession session = service.getSessionById(id);
        byte[] pdfBytes = pdfReportGenerator.generateReport(session);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "vitals-report-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}