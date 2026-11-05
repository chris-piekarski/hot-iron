package hotiron.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import hotiron.core.OperatorBrief;
import net.miginfocom.swing.MigLayout;

/**
 * Last agent note for the visible Spectrum tools face. Shared — not
 * copied into each sniff panel.
 */
public final class OperatorBriefPane extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color INK = new Color(210, 214, 200);
	private static final Color DIM = new Color(120, 124, 118);
	private static final Color BACK = new Color(16, 16, 18);

	private final JTextArea area = new JTextArea();
	private OperatorBrief shown;

	public OperatorBriefPane()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 0, fill", "[grow,fill]", "[grow,fill]"));
		setBorder(BorderFactory.createTitledBorder("Agent brief"));
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(OperatorTheme.ui(12f));
		area.setBackground(BACK);
		area.setForeground(DIM);
		area.setCaretColor(INK);
		area.setText("No agent brief");
		JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, "grow");
		collapse(true);
	}

	public synchronized void setBrief(OperatorBrief brief)
	{
		shown = brief;
		if (brief == null || brief.isEmpty())
		{
			area.setForeground(DIM);
			area.setText("No agent brief");
			collapse(true);
			return;
		}
		area.setForeground(INK);
		StringBuilder sb = new StringBuilder();
		if (!brief.title.isEmpty())
			sb.append(brief.title);
		if (!brief.body.isEmpty())
		{
			if (sb.length() > 0)
				sb.append('\n');
			sb.append(brief.body);
		}
		area.setText(sb.toString());
		area.setCaretPosition(0);
		collapse(false);
	}

	public synchronized OperatorBrief shown()
	{
		return shown;
	}

	private void collapse(boolean empty)
	{
		int h = empty ? 56 : OperatorLayout.BRIEF_HEIGHT;
		Dimension d = new Dimension(OperatorLayout.TOOLS_WIDTH - 16, h);
		setPreferredSize(d);
		setMinimumSize(new Dimension(120, empty ? 48 : 72));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, empty ? 64 : OperatorLayout.BRIEF_HEIGHT));
		revalidate();
	}
}
