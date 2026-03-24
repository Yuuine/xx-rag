package yuuine.xxrag.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupBanner implements CommandLineRunner {

    @Value("${server.port}")
    private int port;

    @Override
    public void run(@NotNull String... args) {

        String baseUrl = "http://localhost:" + port;

        log.info("\n" +
                "==============================================\n" +
                "               应用启动成功\n" +
                "----------------------------------------------\n" +
                "        访问地址: {} \n" +
                "==============================================\n", baseUrl);
    }
}