package yuuine.xxrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.modulith.ApplicationModule;

@SpringBootApplication
@ApplicationModule
@EnableFeignClients(basePackages = "yuuine.xxrag.app.client")
@ComponentScan(basePackages = {
        "yuuine.xxrag.app",
        "yuuine.xxrag.inference",
        "yuuine.xxrag.ingestion",
        "yuuine.xxrag.vector"
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        yuuine.xxrag.ingestion.exception.GlobalExceptionHandler.class
}))
public class XxRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxRagApplication.class, args);
    }

}
