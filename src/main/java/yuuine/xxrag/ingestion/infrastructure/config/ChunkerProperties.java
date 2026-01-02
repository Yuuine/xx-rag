package yuuine.xxrag.ingestion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "app.text-chunker")
@Data
public class ChunkerProperties {

    private int chunkSize = 512;
    private int overlap = 100;
}
