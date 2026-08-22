package hotiron.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens a URL in the user's browser. {@link Desktop#browse} is often
 * unsupported on WSL even when {@link Desktop#isDesktopSupported()} is true.
 */
public final class DesktopBrowse {
	private DesktopBrowse() {
	}

	public static void open(String url) throws IOException {
		if (url == null || url.isEmpty())
			throw new IOException("empty url");
		if (browseWithDesktop(url))
			return;
		IOException last = null;
		List<String[]> candidates = fallbackCommands(url);
		for (int i = 0; i < candidates.size(); i++) {
			try {
				new ProcessBuilder(candidates.get(i)).start();
				return;
			} catch (IOException e) {
				last = e;
			}
		}
		if (last != null)
			throw last;
		throw new IOException("no browser handler for " + url);
	}

	static boolean browseWithDesktop(String url) {
		if (!Desktop.isDesktopSupported())
			return false;
		Desktop desktop = Desktop.getDesktop();
		if (!desktop.isSupported(Desktop.Action.BROWSE))
			return false;
		try {
			desktop.browse(URI.create(url));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static List<String[]> fallbackCommands(String url) {
		List<String[]> cmds = new ArrayList<String[]>();
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win")) {
			cmds.add(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
			cmds.add(new String[] { "cmd", "/c", "start", "", url });
		} else {
			cmds.add(new String[] { "xdg-open", url });
			cmds.add(new String[] { "wslview", url });
			cmds.add(new String[] { "/mnt/c/Windows/System32/rundll32.exe", "url.dll,FileProtocolHandler", url });
		}
		return cmds;
	}
}
