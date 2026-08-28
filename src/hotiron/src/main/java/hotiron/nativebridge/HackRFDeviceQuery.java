package hotiron.nativebridge;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

import hotiron.core.RadioIdentity;

/**
 * Reads HackRF firmware / USB API / board ID through the same
 * {@code hackrf-sweep} shared library the analyzer loads. Does not start a
 * sweep.
 */
public final class HackRFDeviceQuery {
	public static final String MIN_FIRMWARE = "2024.02.1";
	public static final int MIN_USB_API = 0x0100;
	public static final int HACKRF_SUCCESS = 0;

	private static final Pattern FIRMWARE = Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d+)");

	private HackRFDeviceQuery() {
	}

	public static final class Info {
		public final int initResult;
		public final int openResult;
		public final String firmware;
		public final int usbApi;
		public final int boardId;
		public final String boardName;
		public final String libraryRelease;
		public final String libraryVersion;
		public final String serialHex;

		Info(int initResult, int openResult, String firmware, int usbApi, int boardId, String boardName,
				String libraryRelease, String libraryVersion, String serialHex) {
			this.initResult = initResult;
			this.openResult = openResult;
			this.firmware = firmware;
			this.usbApi = usbApi;
			this.boardId = boardId;
			this.boardName = boardName;
			this.libraryRelease = libraryRelease;
			this.libraryVersion = libraryVersion;
			this.serialHex = serialHex;
		}

		public RadioIdentity toIdentity() {
			if (!opened())
				return RadioIdentity.ABSENT;
			return RadioIdentity.of(boardName, serialHex, firmware, usbApi > 0 ? usbApiString() : null);
		}

		public boolean opened() {
			return initResult == HACKRF_SUCCESS && openResult == HACKRF_SUCCESS;
		}

		public int usbApiMajor() {
			return (usbApi >> 8) & 0xFF;
		}

		public int usbApiMinor() {
			return usbApi & 0xFF;
		}

		public String usbApiString() {
			return usbApiMajor() + "." + String.format("%02d", Integer.valueOf(usbApiMinor()));
		}
	}

	public static Info query() {
		NativeLibrary lib = HackRFSweepNativeBridge.JNA_NATIVE_LIB;
		int init = invokeInt(lib, "hackrf_init");
		String release = invokeCString(lib, "hackrf_library_release");
		String libVer = invokeCString(lib, "hackrf_library_version");
		if (init != HACKRF_SUCCESS)
			return new Info(init, -1, null, 0, 0xFF, null, release, libVer, null);

		PointerByReference deviceRef = new PointerByReference();
		int open = lib.getFunction("hackrf_open").invokeInt(new Object[] { deviceRef });
		if (open != HACKRF_SUCCESS)
			return new Info(init, open, null, 0, 0xFF, null, release, libVer, null);

		Pointer device = deviceRef.getValue();
		try {
			byte[] versionBuf = new byte[256];
			int verRc = lib.getFunction("hackrf_version_string_read").invokeInt(
					new Object[] { device, versionBuf, Byte.valueOf((byte) 255) });
			String firmware = verRc == HACKRF_SUCCESS ? cString(versionBuf) : null;

			ShortByReference apiRef = new ShortByReference();
			int apiRc = lib.getFunction("hackrf_usb_api_version_read").invokeInt(new Object[] { device, apiRef });
			int usbApi = apiRc == HACKRF_SUCCESS ? (apiRef.getValue() & 0xFFFF) : 0;

			ByteByReference boardRef = new ByteByReference();
			int boardRc = lib.getFunction("hackrf_board_id_read").invokeInt(new Object[] { device, boardRef });
			int boardId = boardRc == HACKRF_SUCCESS ? (boardRef.getValue() & 0xFF) : 0xFF;
			String boardName = invokeCString(lib, "hackrf_board_id_name", Integer.valueOf(boardId));

			String serialHex = readSerial(lib, device);

			return new Info(init, open, firmware, usbApi, boardId, boardName, release, libVer, serialHex);
		} finally {
			lib.getFunction("hackrf_close").invokeInt(new Object[] { device });
		}
	}

	/** MCU unique ID as 32 hex digits — same value GSG prints as the USB serial. */
	private static String readSerial(NativeLibrary lib, Pointer device) {
		try {
			PartIdSerialno partid = new PartIdSerialno();
			int rc = lib.getFunction("hackrf_board_partid_serialno_read").invokeInt(new Object[] { device, partid });
			if (rc != HACKRF_SUCCESS)
				return null;
			return RadioIdentity.formatMcuSerial(new int[] {
					partid.serial0, partid.serial1, partid.serial2, partid.serial3 });
		} catch (UnsatisfiedLinkError | RuntimeException e) {
			return null;
		}
	}

	/** Matches libhackrf {@code read_partid_serialno_t}. */
	public static class PartIdSerialno extends Structure {
		public int part0;
		public int part1;
		public int serial0;
		public int serial1;
		public int serial2;
		public int serial3;

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("part0", "part1", "serial0", "serial1", "serial2", "serial3");
		}
	}

	public static String errorName(int code) {
		String name = invokeCString(HackRFSweepNativeBridge.JNA_NATIVE_LIB, "hackrf_error_name",
				Integer.valueOf(code));
		return name != null ? name : Integer.toString(code);
	}

	/** Extract {@code YYYY.M.P} from a firmware string, or {@code null}. */
	public static int[] parseFirmwareParts(String raw) {
		if (raw == null)
			return null;
		Matcher m = FIRMWARE.matcher(raw.trim());
		if (!m.find())
			return null;
		return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)) };
	}

	public static int compareFirmware(String a, String b) {
		int[] pa = parseFirmwareParts(a);
		int[] pb = parseFirmwareParts(b);
		if (pa == null || pb == null)
			throw new IllegalArgumentException("unparseable firmware: '" + a + "' vs '" + b + "'");
		for (int i = 0; i < 3; i++) {
			if (pa[i] != pb[i])
				return pa[i] < pb[i] ? -1 : 1;
		}
		return 0;
	}

	public static boolean meetsMinimumFirmware(String raw, String minimum) {
		if (parseFirmwareParts(raw) == null || parseFirmwareParts(minimum) == null)
			return false;
		return compareFirmware(raw, minimum) >= 0;
	}

	public static boolean isKnownHackrfBoard(int boardId) {
		return boardId == 1 || boardId == 2 || boardId == 3 || boardId == 4
				|| boardId == 5;
	}

	/**
	 * True when a HackRF usbfs node is enumerated (sysfs 1d50:6089 / 604b /
	 * cc15). No libhackrf open — safe while a sweep holds the radio.
	 */
	public static boolean usbEnumerated() {
		File root = new File("/sys/bus/usb/devices");
		File[] kids = root.listFiles();
		if (kids == null)
			return false;
		for (int i = 0; i < kids.length; i++) {
			String vendor = readSysfs(new File(kids[i], "idVendor"));
			String product = readSysfs(new File(kids[i], "idProduct"));
			if (!"1d50".equalsIgnoreCase(vendor))
				continue;
			if ("6089".equalsIgnoreCase(product) || "604b".equalsIgnoreCase(product)
					|| "cc15".equalsIgnoreCase(product))
				return true;
		}
		return false;
	}

	private static String readSysfs(File f) {
		if (f == null || !f.isFile())
			return "";
		try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
			byte[] buf = new byte[16];
			int n = in.read(buf);
			if (n <= 0)
				return "";
			return new String(buf, 0, n, java.nio.charset.StandardCharsets.US_ASCII).trim();
		} catch (java.io.IOException e) {
			return "";
		}
	}

	/**
	 * USB serials from {@code hackrf_device_list}. Does not open RX, but
	 * still talks to libusb — do not call this while a sweep is streaming.
	 */
	public static List<String> listSerials() {
		try {
			NativeLibrary lib = HackRFSweepNativeBridge.JNA_NATIVE_LIB;
			invokeInt(lib, "hackrf_init");
			Pointer raw = lib.getFunction("hackrf_device_list").invokePointer(new Object[0]);
			if (raw == null)
				return Collections.emptyList();
			try {
				DeviceList list = new DeviceList(raw);
				if (list.devicecount < 1 || list.serial_numbers == null)
					return Collections.emptyList();
				Pointer[] ptrs = list.serial_numbers.getPointerArray(0, list.devicecount);
				List<String> out = new ArrayList<>();
				for (Pointer p : ptrs) {
					if (p == null)
						continue;
					String s = p.getString(0);
					if (s != null && !s.isEmpty())
						out.add(s);
				}
				return out;
			} finally {
				lib.getFunction("hackrf_device_list_free").invokeVoid(new Object[] { raw });
			}
		} catch (UnsatisfiedLinkError | RuntimeException e) {
			return Collections.emptyList();
		}
	}

	public static class DeviceList extends Structure {
		public Pointer serial_numbers;
		public Pointer usb_board_ids;
		public Pointer usb_device_index;
		public int devicecount;
		public Pointer usb_devices;
		public int usb_devicecount;

		public DeviceList(Pointer p) {
			super(p);
			read();
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("serial_numbers", "usb_board_ids", "usb_device_index", "devicecount",
					"usb_devices", "usb_devicecount");
		}
	}

	private static int invokeInt(NativeLibrary lib, String name) {
		return lib.getFunction(name).invokeInt(new Object[0]);
	}

	private static String invokeCString(NativeLibrary lib, String name, Object... args) {
		Pointer p = lib.getFunction(name).invokePointer(args);
		return p == null ? null : p.getString(0);
	}

	private static String cString(byte[] buf) {
		int n = 0;
		while (n < buf.length && buf[n] != 0)
			n++;
		if (n == 0)
			return "";
		try {
			return new String(buf, 0, n, "US-ASCII").trim();
		} catch (java.io.UnsupportedEncodingException e) {
			return new String(buf, 0, n).trim();
		}
	}
}
