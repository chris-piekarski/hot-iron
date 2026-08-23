package hotiron.ui;

import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import hotiron.core.FrequencyRange;
import hotiron.core.SpectrumZoom;
import net.miginfocom.swing.MigLayout;

/**
 * One sweep-window control: readout you can type into, pan, and zoom.
 * Replaces the old start/end digit wheels.
 */
public final class FrequencyRangePanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final String CARD_READ = "read";
	private static final String CARD_EDIT = "edit";

	private FrequencyRange range = new FrequencyRange(QuickSelectPreset.WIFI_2.startMHz,
			QuickSelectPreset.WIFI_2.endMHz);
	private final JLabel readout = new JLabel("", SwingConstants.CENTER);
	private final JLabel span = new JLabel("", SwingConstants.CENTER);
	private final JTextField edit = new JTextField();
	private final CardLayout cards = new CardLayout();
	private final JPanel display = new JPanel(cards);
	private final JButton panLeft = new JButton("◀");
	private final JButton panRight = new JButton("▶");
	private final JButton zoomIn = new JButton("+");
	private final JButton zoomOut = new JButton("−");

	public FrequencyRangePanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, wrap 1, gapy 2", "[grow,fill]", "[][]"));
		readout.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
		readout.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		span.setFont(span.getFont().deriveFont(Font.PLAIN, 11f));
		span.setEnabled(false);
		edit.setFont(readout.getFont());
		edit.setHorizontalAlignment(SwingConstants.CENTER);
		ExclusiveToolTip.setText(edit, "Type a range like 88-108 or 2402 2472, or a center like 97");
		ExclusiveToolTip.setText(readout, "Click to type a range. Plot drag/scroll also zooms.");
		JPanel readCard = new JPanel(new MigLayout("insets 2, wrap 1", "[grow,fill]", "[][]"));
		readCard.add(readout);
		readCard.add(span);
		display.add(readCard, CARD_READ);
		display.add(edit, CARD_EDIT);
		add(display);
		JPanel keys = new JPanel(new MigLayout("insets 0", "[grow][grow][grow][grow]", "[]"));
		panLeft.setName("pan-left");
		panRight.setName("pan-right");
		zoomIn.setName("zoom-in");
		zoomOut.setName("zoom-out");
		ExclusiveToolTip.setText(panLeft, "Pan lower in frequency (¼ of the span)");
		ExclusiveToolTip.setText(panRight, "Pan higher in frequency (¼ of the span)");
		ExclusiveToolTip.setText(zoomIn, "Zoom in (half the span, same center)");
		ExclusiveToolTip.setText(zoomOut, "Zoom out (double the span, same center)");
		keys.add(panLeft, "growx");
		keys.add(zoomOut, "growx");
		keys.add(zoomIn, "growx");
		keys.add(panRight, "growx");
		add(keys);
		panLeft.addActionListener(e -> apply(SpectrumZoom.pan(range, -0.25)));
		panRight.addActionListener(e -> apply(SpectrumZoom.pan(range, 0.25)));
		zoomIn.addActionListener(e -> apply(SpectrumZoom.around(range, mid(), SpectrumZoom.ZOOM_IN_FACTOR)));
		zoomOut.addActionListener(e -> apply(SpectrumZoom.expand(range)));
		readout.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				beginEdit();
			}
		});
		edit.addActionListener(e -> commitEdit());
		edit.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commitEdit();
			}
		});
		edit.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					cards.show(display, CARD_READ);
					e.consume();
				}
			}
		});
		refreshText();
	}

	public FrequencyRange getRange()
	{
		return range;
	}

	public void setRange(FrequencyRange next)
	{
		if (next == null)
			return;
		FrequencyRange clamped = SpectrumZoom.clamp(next.getStartMHz(), next.getEndMHz());
		FrequencyRange old = range;
		if (clamped.equals(old))
		{
			refreshText();
			return;
		}
		range = clamped;
		refreshText();
		cards.show(display, CARD_READ);
		firePropertyChange("value", old, range);
	}

	public void addRangeListener(PropertyChangeListener listener)
	{
		addPropertyChangeListener("value", listener);
	}

	JButton panLeftButton()
	{
		return panLeft;
	}

	JButton zoomOutButton()
	{
		return zoomOut;
	}

	JButton zoomInButton()
	{
		return zoomIn;
	}

	JButton panRightButton()
	{
		return panRight;
	}

	JLabel readoutLabel()
	{
		return readout;
	}

	JTextField editField()
	{
		return edit;
	}

	void beginEdit()
	{
		edit.setText(range.getStartMHz() + "-" + range.getEndMHz());
		cards.show(display, CARD_EDIT);
		edit.requestFocusInWindow();
		edit.selectAll();
	}

	private double mid()
	{
		return (range.getStartMHz() + range.getEndMHz()) / 2.0;
	}

	private void apply(FrequencyRange next)
	{
		setRange(next);
	}

	private void commitEdit()
	{
		Optional<FrequencyRange> parsed = FrequencyRange.parse(edit.getText(), Math.max(1, range.spanMHz()));
		parsed.ifPresent(this::setRange);
		cards.show(display, CARD_READ);
	}

	private void refreshText()
	{
		readout.setText(range.getStartMHz() + " – " + range.getEndMHz() + " MHz");
		span.setText(range.spanMHz() + " MHz span");
	}
}
