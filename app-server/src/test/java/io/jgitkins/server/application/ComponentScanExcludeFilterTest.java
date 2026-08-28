package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.JGitkinsServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;

/**
 * Pins the component-scan configuration on the application class.
 *
 * <p>The application used to declare {@code @ComponentScan("io.jgitkins.server")} alongside
 * {@code @SpringBootApplication}. Both cover the same package, so the explicit annotation looked
 * redundant and harmless. It was neither: an explicit {@code @ComponentScan} replaces the filters
 * {@code @SpringBootApplication} contributes, including {@link TypeExcludeFilter}.
 *
 * <p>Two things broke silently as a result. Any {@code @Configuration} in test sources under
 * {@code io.jgitkins.server} was registered into the context of every {@code @SpringBootTest} that
 * booted the application, so a test-local stub could install beans into the production context.
 * And every {@code @WebMvcTest} in this module stopped being a slice: with no
 * {@code TypeExcludeFilter}, the whole application loaded, and the controller tests passed because
 * of it rather than in spite of it.
 *
 * <p>Re-adding an explicit {@code @ComponentScan} would reopen both holes without failing anything
 * obvious, so this test fails if one appears.
 */
class ComponentScanExcludeFilterTest {

    @Test
    void applicationDoesNotDeclareAnExplicitComponentScan() {
        assertThat(JGitkinsServerApplication.class.getAnnotation(ComponentScan.class))
                .as("an explicit @ComponentScan replaces the @SpringBootApplication filters, including "
                        + "TypeExcludeFilter; without it @WebMvcTest loads the whole application and test "
                        + "@Configuration classes leak into the production context")
                .isNull();
    }

    @Test
    void applicationRemainsSpringBootApplicationScanningItsOwnPackage() {
        assertThat(JGitkinsServerApplication.class.getAnnotation(SpringBootApplication.class))
                .as("the default scan of the declaring package replaces the removed explicit one, and it "
                        + "is what carries TypeExcludeFilter")
                .isNotNull();
        assertThat(JGitkinsServerApplication.class.getPackageName())
                .as("the default scan covers the declaring package, so it must be the package the removed "
                        + "explicit scan named")
                .isEqualTo("io.jgitkins.server");
    }
}
