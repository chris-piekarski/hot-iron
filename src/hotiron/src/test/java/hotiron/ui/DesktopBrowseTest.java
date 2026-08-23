package hotiron.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.Version;

class DesktopBrowseTest {

	@Test
	void homepageUrlIsHttpsGithub() {
		assertTrue(Version.url.startsWith("https://"));
		assertTrue(Version.url.contains("github.com/chris-piekarski/hot-iron"));
	}

	@Test
	void emptyUrlThrows() {
		assertThrows(Exception.class, () -> DesktopBrowse.open(""));
	}

	@Test
	void linuxFallbackPrefersXdgOpen() {
		String os = System.getProperty("os.name");
		List<String[]> cmds = DesktopBrowse.fallbackCommands(Version.url);
		assertFalse(cmds.isEmpty());
		if (os != null && os.toLowerCase().contains("win")) {
			assertEquals("rundll32", cmds.get(0)[0]);
		} else {
			assertEquals("xdg-open", cmds.get(0)[0]);
			assertEquals(Version.url, cmds.get(0)[1]);
		}
	}
}
