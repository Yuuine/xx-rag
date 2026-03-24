package yuuine.xxrag.rerank.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yuuine.xxrag.rerank.config.RerankProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RerankService {

    private final RerankProperties properties;
    private final RestTemplate restTemplate;
    
    private boolean initialized = false;

    public RerankService(RerankProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Rerank service is disabled");
            return;
        }

        try {
            log.info("Initializing Rerank service with Python microservice at: {}", properties.getServiceUrl());
            
            // 检查 Python 服务是否健康
            String healthUrl = properties.getServiceUrl() + "/health";
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Python Rerank microservice is healthy");
                    initialized = true;
                } else {
                    log.warn("Python Rerank microservice health check failed with status: {}", response.getStatusCode());
                    initialized = false;
                }
            } catch (Exception e) {
                log.warn("Could not connect to Python Rerank microservice. Make sure it's running on {}", properties.getServiceUrl());
                log.warn("Rerank functionality will be disabled until the service is available");
                initialized = false;
            }
            
            log.info("Rerank service initialized. Status: {}", initialized ? "READY" : "NOT READY");
        } catch (Exception e) {
            log.error("Failed to initialize Rerank service, rerank will be disabled", e);
            initialized = false;
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled() && initialized;
    }

    public List<Integer> rerank(String query, List<String> documents) {
        return rerank(query, documents, properties.getTopK());
    }

    public List<Integer> rerank(String query, List<String> documents, int topK) {
        if (!isEnabled()) {
            log.debug("Rerank is not available, returning original indices (first {})", Math.min(topK, documents.size()));
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, documents.size()); i++) {
                indices.add(i);
            }
            return indices;
        }

        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            log.debug("Reranking {} documents with query: {}", documents.size(), query);

            // 构建请求
            String rerankUrl = properties.getServiceUrl() + "/rerank";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("documents", documents);
            requestBody.put("top_k", Math.min(topK, documents.size()));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 调用 Python 服务
            ResponseEntity<RerankResponse> response = restTemplate.postForEntity(
                rerankUrl, 
                request, 
                RerankResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Integer> rerankedIndices = new ArrayList<>();
                for (RerankResult result : response.getBody().getResults()) {
                    rerankedIndices.add(result.getIndex());
                }
                log.debug("Rerank completed, returning {} indices", rerankedIndices.size());
                return rerankedIndices;
            } else {
                log.warn("Rerank service returned unexpected status: {}", response.getStatusCode());
                List<Integer> fallbackIndices = new ArrayList<>();
                for (int i = 0; i < Math.min(topK, documents.size()); i++) {
                    fallbackIndices.add(i);
                }
                return fallbackIndices;
            }
            
        } catch (Exception e) {
            log.error("Error during rerank, falling back to original indices", e);
            List<Integer> fallbackIndices = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, documents.size()); i++) {
                fallbackIndices.add(i);
            }
            return fallbackIndices;
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Rerank service shutting down");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RerankResult {
        private int index;
        private String document;
        private double score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RerankResponse {
        private List<RerankResult> results;
    }
}
