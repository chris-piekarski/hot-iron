package hotiron.hw;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Marks a test that needs a HackRF. Surefire {@code excludedGroups=hardware}
 * keeps these out of {@code make test}. Run with {@code make test-hw}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("hardware")
@Test
public @interface HardwareTest {
}
