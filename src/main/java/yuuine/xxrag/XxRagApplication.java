package yuuine.xxrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import yuuine.xxrag.ingestion.exception.GlobalExceptionHandler;

@SpringBootApplication
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
public class XxRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxRagApplication.class, args);
    }
}