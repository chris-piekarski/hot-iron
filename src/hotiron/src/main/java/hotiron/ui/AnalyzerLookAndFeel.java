package hotiron.ui;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Installs the app look-and-feel before any Swing widget is created.
 * Safe to call more than once (tests and {@code main} both invoke it).
 */
public final class AnalyzerLookAndFeel {
	private static volatile boolean installed;

	private AnalyzerLookAndFeel() {
	}

	public static void install() {
		if (installed)
			return;
		synchronized (AnalyzerLookAndFeel.class) {
			if (installed)
				return;
			FlatDarkLaf.setup();
			ExclusiveToolTip.installShared();
			installed = true;
		}
	}

	static boolean isInstalled() {
		return installed;
	}
}
