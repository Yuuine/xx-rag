package yuuine.xxrag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yuuine.xxrag.app.client.IngestionClient;
import yuuine.xxrag.app.client.VectorClient;
import yuuine.xxrag.app.client.InferenceClient;

@SpringBootTest
class XxRagApplicationTests {

    @MockitoBean
    private IngestionClient ingestionClient;

    @MockitoBean
    private VectorClient vectorClient;

    @MockitoBean
    private InferenceClient inferenceClient;

    @Test
    void contextLoads() {
    }

    @Test
    void verifyModuleStructure(ApplicationModules modules) {
        // 验证Spring Modulith是否正确检测到模块结构
        modules.verify();
    }

}
