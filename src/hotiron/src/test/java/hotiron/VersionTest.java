package hotiron;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void testVersionConstants() {
        assertNotNull(Version.version);
        assertTrue(Version.version.length() > 0);
        assertNotNull(Version.url);
        assertTrue(Version.url.contains("github"));
    }
}
