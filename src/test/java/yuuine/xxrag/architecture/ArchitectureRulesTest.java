package yuuine.xxrag.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "yuuine.xxrag")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule adapter_should_not_directly_depend_on_vector_internal =
            noClasses()
                    .that()
                    .resideInAnyPackage("..controller..", "..websocket..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..vector.application..", "..vector.domain..")
                    .because("adapter 层应通过 app/api 访问能力，避免直连 vector 内部实现");

    @ArchTest
    static final ArchRule adapter_should_not_directly_depend_on_inference_internal =
            noClasses()
                    .that()
                    .resideInAnyPackage("..controller..", "..websocket..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..inference.service..", "..inference.dto..")
                    .because("adapter 层应通过 app/api 访问能力，避免直连 inference 内部实现");

    @ArchTest
    static final ArchRule adapter_should_not_directly_depend_on_ingestion_internal =
            noClasses()
                    .that()
                    .resideInAnyPackage("..controller..", "..websocket..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..ingestion.application..", "..ingestion.domain..", "..ingestion.infrastructure..")
                    .because("adapter 层应通过 app/api 访问能力，避免直连 ingestion 内部实现");

    @ArchTest
    static final ArchRule app_module_should_not_depend_on_other_modules_internal_impl =
            noClasses()
                    .that()
                    .resideInAnyPackage("..app.application..", "..app.api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..vector.application..",
                            "..vector.domain..",
                            "..inference.service..",
                            "..ingestion.application..",
                            "..ingestion.domain..",
                            "..ingestion.infrastructure.."
                    )
                    .because("app 模块应依赖各模块 api，而不是内部实现");
}

