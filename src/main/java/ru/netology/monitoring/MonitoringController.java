package ru.netology.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class MonitoringController {
    private static final long MAX_RETAINED_BYTES = 128L * 1024 * 1024;
    private static final int MAX_REQUEST_MEGABYTES = 32;
    private final MeterRegistry meterRegistry;
    private final List<byte[]> retainedMemory = new ArrayList<>();
    private final AtomicLong retainedBytes = new AtomicLong();

    public MonitoringController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/api/success")
    public ResponseEntity<Map<String, String>> success() {
        recordRequest("success", "200");
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/api/not-found")
    public ResponseEntity<Map<String, String>> notFound() {
        recordRequest("not-found", "404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "not-found"));
    }

    @GetMapping("/api/error")
    public ResponseEntity<Map<String, String>> error() {
        recordRequest("error", "500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error"));
    }

    @PostMapping("/api/memory")
    public ResponseEntity<Map<String, Object>> allocateMemory(@RequestParam(defaultValue = "8") int megabytes) {
        if (megabytes < 1 || megabytes > MAX_REQUEST_MEGABYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "megabytes must be between 1 and 32"));
        }

        long requestedBytes = megabytes * 1024L * 1024L;
        synchronized (retainedMemory) {
            if (retainedBytes.get() + requestedBytes > MAX_RETAINED_BYTES) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "memory allocation limit reached"));
            }
            retainedMemory.add(new byte[(int) requestedBytes]);
            retainedBytes.addAndGet(requestedBytes);
        }

        recordRequest("memory", "200");
        return ResponseEntity.ok(Map.of("allocatedMegabytes", megabytes,
                "retainedMegabytes", retainedBytes.get() / (1024 * 1024)));
    }

    @DeleteMapping("/api/memory")
    public ResponseEntity<Map<String, Long>> clearMemory() {
        synchronized (retainedMemory) {
            retainedMemory.clear();
            retainedBytes.set(0);
        }
        recordRequest("memory-clear", "200");
        return ResponseEntity.ok(Map.of("retainedBytes", retainedBytes.get()));
    }

    private void recordRequest(String endpoint, String status) {
        meterRegistry.counter("app.requests", "endpoint", endpoint, "status", status).increment();
    }
}

