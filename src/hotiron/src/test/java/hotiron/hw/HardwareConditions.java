package hotiron.hw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Presence checks for gated hardware tests. Used by {@code @EnabledIf} so
 * {@code make test} never executes radio/JNA sweep tests.
 */
public final class HardwareConditions {
	public static final String HACKRF_VENDOR = "1d50";
	public static final String HACKRF_PRODUCT = "6089";

	private HardwareConditions() {
	}

	/** JUnit {@code @EnabledIf} entry: HackRF USB is enumerated. */
	public static boolean hackrfUsbPresent() {
		if (sysfsHackrfPresent())
			return true;
		return lsusbHackrfPresent();
	}

	/** JUnit {@code @EnabledIf} entry: USB present and native sweep library exists. */
	public static boolean hackrfAndLibraryPresent() {
		return hackrfUsbPresent() && findNativeLibrary() != null;
	}

	public static boolean sysfsHackrfPresent() {
		File root = new File("/sys/bus/usb/devices");
		File[] kids = root.listFiles();
		if (kids == null)
			return false;
		for (int i = 0; i < kids.length; i++) {
			File dir = kids[i];
			String vendor = readTrimmed(new File(dir, "idVendor"));
			String product = readTrimmed(new File(dir, "idProduct"));
			if (HACKRF_VENDOR.equalsIgnoreCase(vendor) && HACKRF_PRODUCT.equalsIgnoreCase(product))
				return true;
		}
		return false;
	}

	public static boolean lsusbHackrfPresent() {
		try {
			Process p = new ProcessBuilder("lsusb").redirectErrorStream(true).start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = r.readLine()) != null) {
				String lower = line.toLowerCase();
				if (lower.contains("1d50:6089") || lower.contains("hackrf"))
					return true;
			}
			p.waitFor();
		} catch (Exception ignored) {
		}
		return false;
	}

	/**
	 * libusb needs write on the usbfs node. WSL usbipd devices are often root:root rw-rw-r--.
	 */
	public static File findHackrfUsbDeviceNode() {
		File root = new File("/sys/bus/usb/devices");
		File[] kids = root.listFiles();
		if (kids == null)
			return null;
		for (int i = 0; i < kids.length; i++) {
			File dir = kids[i];
			String vendor = readTrimmed(new File(dir, "idVendor"));
			String product = readTrimmed(new File(dir, "idProduct"));
			if (!HACKRF_VENDOR.equalsIgnoreCase(vendor) || !HACKRF_PRODUCT.equalsIgnoreCase(product))
				continue;
			String bus = readTrimmed(new File(dir, "busnum"));
			String dev = readTrimmed(new File(dir, "devnum"));
			if (bus.isEmpty() || dev.isEmpty())
				continue;
			try {
				File node = new File(String.format("/dev/bus/usb/%03d/%03d", Integer.parseInt(bus), Integer.parseInt(dev)));
				if (node.exists())
					return node;
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	public static boolean hackrfUsbNodeWritable() {
		File node = findHackrfUsbDeviceNode();
		return node != null && node.canWrite();
	}

	public static File findNativeLibrary() {
		String env = System.getenv("HACKRF_SWEEP_LIB_DIR");
		if (env != null && !env.isEmpty()) {
			File fromEnv = new File(env, nativeLibraryFileName());
			if (fromEnv.isFile())
				return fromEnv;
		}
		String[] relatives = new String[] {
				"build/hotiron/lib/linux-x86-64/libhackrf-sweep.so",
				"../build/hotiron/lib/linux-x86-64/libhackrf-sweep.so",
				"../../src/hotiron/build/hotiron/lib/linux-x86-64/libhackrf-sweep.so",
				"lib/linux-x86-64/libhackrf-sweep.so",
		};
		File cwd = new File(System.getProperty("user.dir", "."));
		for (int i = 0; i < relatives.length; i++) {
			File f = new File(cwd, relatives[i]);
			if (f.isFile())
				return f.getAbsoluteFile();
		}
		return null;
	}

	public static String nativeLibraryFileName() {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win"))
			return "hackrf-sweep.dll";
		return "libhackrf-sweep.so";
	}

	private static String readTrimmed(File f) {
		if (f == null || !f.isFile())
			return "";
		FileInputStream in = null;
		try {
			in = new FileInputStream(f);
			byte[] buf = new byte[32];
			int n = in.read(buf);
			if (n <= 0)
				return "";
			return new String(buf, 0, n, "UTF-8").trim();
		} catch (IOException e) {
			return "";
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignored) {
				}
			}
		}
	}
}
