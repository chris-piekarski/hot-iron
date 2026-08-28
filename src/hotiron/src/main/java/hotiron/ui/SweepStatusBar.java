package hotiron.ui;

import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Full-width strip under the plots. Replaces the old waterfall HUD overlay.
 */
public class SweepStatusBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JLabel mode = field("Panel  RF waterfall");
	private final JLabel resolution = field("Resolution  —");
	private final JLabel bins = field("FFT bins  —");
	private final JLabel rate = field("Waterfall  —");
	private final JLabel peak = field("Peak  —");
	private final JLabel mcp = field("MCP  off");

	public SweepStatusBar() {
		AnalyzerLookAndFeel.install();
		setLayout(new FlowLayout(FlowLayout.LEFT, 16, 2));
		setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		mode.setFont(mode.getFont().deriveFont(Font.BOLD, 12f));
		add(mode);
		add(sep());
		add(resolution);
		add(sep());
		add(bins);
		add(sep());
		add(rate);
		add(sep());
		add(peak);
		add(sep());
		add(mcp);
	}

	public void installAutoSweep(JCheckBox auto)
	{
		if (auto == null)
			return;
		add(auto, 2);
		add(sep(), 3);
	}

	public void setMcp(hotiron.core.McpStatus status) {
		mcp.setText(status == null ? hotiron.core.McpStatus.OFF.barText() : status.barText());
	}

	public void setSweepInfo(double rbwHz, int fftBins, double waterfallFps, Double peakDbm) {
		setSweepInfo(rbwHz, fftBins, waterfallFps, peakDbm, false);
	}

	public void setSweepInfo(double rbwHz, int fftBins, double waterfallFps, Double peakDbm, boolean audio) {
		setSweepInfo(rbwHz, fftBins, waterfallFps, peakDbm, audio, false);
	}

	public void setSweepInfo(double rbwHz, int fftBins, double waterfallFps, Double peakDbm, boolean audio,
			boolean video) {
		setSweepInfo(rbwHz, fftBins, waterfallFps, peakDbm, audio, video, false);
	}

	public void setSweepInfo(double rbwHz, int fftBins, double waterfallFps, Double peakDbm, boolean audio,
			boolean video, boolean nfc) {
		mode.setText("Panel  " + WaterfallPlot.modeBanner(audio, video, nfc));
		resolution.setText("Resolution  " + formatHz(rbwHz));
		bins.setText("FFT bins  " + formatBins(fftBins));
		String ratePrefix = nfc ? "NFC  " : video ? "Video  " : audio ? "Audio  " : "Waterfall  ";
		rate.setText(ratePrefix + formatFps(waterfallFps));
		peak.setText("Peak  " + ((audio || video || nfc) ? formatPeakDbfs(peakDbm) : formatPeakDbm(peakDbm)));
	}

	public void setListenDualInfo(double rbwHz, int fftBins, double waterfallFps, Double peakDbfs) {
		mode.setText("Panel  " + WaterfallPlot.modeBannerListenDual());
		resolution.setText("Resolution  " + formatHz(rbwHz));
		bins.setText("FFT bins  " + formatBins(fftBins));
		rate.setText("Listen  " + formatFps(waterfallFps));
		peak.setText("Peak  " + formatPeakDbfs(peakDbfs));
	}

	public static String formatHz(double hz) {
		if (!(hz > 0) || Double.isNaN(hz) || Double.isInfinite(hz))
			return "—";
		if (hz >= 1_000_000d)
			return String.format("%.2f MHz", Double.valueOf(hz / 1_000_000d));
		if (hz >= 1000d)
			return String.format("%.1f kHz", Double.valueOf(hz / 1000d));
		return String.format("%.0f Hz", Double.valueOf(hz));
	}

	public static String formatBins(int n) {
		if (n <= 0)
			return "—";
		return String.format("%,d", Integer.valueOf(n));
	}

	public static String formatFps(double fps) {
		if (!(fps > 0) || Double.isNaN(fps) || Double.isInfinite(fps))
			return "—";
		return String.format("%.0f fps", Double.valueOf(fps));
	}

	public static String formatPeakDbm(Double peakDbm) {
		if (peakDbm == null || peakDbm.isNaN() || peakDbm.isInfinite())
			return "—";
		return String.format("%.1f dBm", peakDbm);
	}

	public static String formatPeakDbfs(Double peakDb) {
		if (peakDb == null || peakDb.isNaN() || peakDb.isInfinite())
			return "—";
		return String.format("%.1f dBFS", peakDb);
	}

	String getModeText() {
		return mode.getText();
	}

	String getPeakText() {
		return peak.getText();
	}

	String getMcpText() {
		return mcp.getText();
	}

	private static JLabel field(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
		return l;
	}

	private static JLabel sep() {
		JLabel l = new JLabel("·");
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setEnabled(false);
		return l;
	}
}
