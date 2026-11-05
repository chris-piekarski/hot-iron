package hotiron.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import hotiron.core.BleFrame;
import hotiron.core.BleMacRow;
import hotiron.core.BleMacTracker;
import net.miginfocom.swing.MigLayout;

/**
 * Parallel nRF BLE sniff. One stable row per MAC; rolling PDU/RSSI
 * on the right. HackRF keeps sweeping 2400–2484.
 */
public final class BleSniffPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color LCD = new Color(120, 200, 255);
	private static final Color LCD_DIM = new Color(70, 110, 140);

	private final JLabel title = new JLabel("2.4 BLE", SwingConstants.CENTER);
	private final JLabel hint = new JLabel("nRF ACM · HackRF stays on sweep", SwingConstants.CENTER);
	private final JLabel status = new JLabel("idle", SwingConstants.CENTER);
	private final JButton sniff = new JButton("Sniff");
	private final BleMacTracker tracker = new BleMacTracker();
	private final DefaultTableModel model = new DefaultTableModel(new Object[] { "MAC", "live" }, 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	};
	private final JTable table = new JTable(model);
	private Runnable onSniff;
	private Consumer<BleMacRow> onSelect;

	public BleSniffPanel()
	{
		AnalyzerLookAndFeel.install();
		setLayout(new MigLayout("insets 4 6 6 6, wrap 1, fill", "[grow,fill]", "[][][][][grow]"));
		setBorder(BorderFactory.createTitledBorder("BLE sniff"));
		title.setFont(OperatorTheme.ui(Font.BOLD, 18f));
		title.setForeground(LCD_DIM);
		hint.setForeground(LCD_DIM);
		status.setForeground(LCD_DIM);
		ExclusiveToolTip.setText(sniff,
				"Open the nRF J-Link CDC and decode Nordic sniffer UART frames. Sets sweep to 2400–2484 MHz. Does not park the HackRF. Click a MAC to mark that channel on the spectrum and waterfall.");
		table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		table.setRowHeight(18);
		table.setShowGrid(false);
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.getTableHeader().setReorderingAllowed(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(148);
		table.getColumnModel().getColumn(0).setMaxWidth(168);
		table.getColumnModel().getColumn(1).setPreferredWidth(260);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (e.getValueIsAdjusting())
				return;
			fireSelect();
		});
		sniff.addActionListener(ev -> {
			if (onSniff != null)
				onSniff.run();
		});
		add(title);
		add(hint);
		add(status);
		add(sniff, "growx");
		add(new JScrollPane(table), "grow");
	}

	public JButton sniffButton()
	{
		return sniff;
	}

	public void setOnSniff(Runnable onSniff)
	{
		this.onSniff = onSniff;
	}

	public void setOnSelect(Consumer<BleMacRow> onSelect)
	{
		this.onSelect = onSelect;
	}

	public BleMacRow selected()
	{
		int row = table.getSelectedRow();
		List<BleMacRow> rows = tracker.rows();
		if (row < 0 || row >= rows.size())
			return null;
		return rows.get(row);
	}

	public void setSniffing(boolean on)
	{
		title.setForeground(on ? LCD : LCD_DIM);
		status.setForeground(on ? LCD : LCD_DIM);
		sniff.setText(on ? "Stop" : "Sniff");
		if (!on)
			status.setText("idle");
	}

	public void setStatus(String text)
	{
		status.setText(text == null || text.isBlank() ? "idle" : text);
	}

	public void resetStations()
	{
		tracker.reset();
		syncTable(null);
	}

	public void ingest(BleFrame frame)
	{
		String keep = selectedAddress();
		tracker.ingest(frame, frame == null ? System.currentTimeMillis() : Math.max(frame.timestampMs,
				System.currentTimeMillis()));
		syncTable(keep);
	}

	public void setFrames(List<BleFrame> next)
	{
		String keep = selectedAddress();
		tracker.reset();
		if (next != null)
		{
			for (BleFrame f : next)
				tracker.ingest(f, f.timestampMs);
		}
		tracker.expire(System.currentTimeMillis());
		syncTable(keep);
	}

	private String selectedAddress()
	{
		BleMacRow row = selected();
		return row == null ? null : row.address;
	}

	private void syncTable(String keepAddress)
	{
		List<BleMacRow> rows = tracker.rows();
		while (model.getRowCount() > rows.size())
			model.removeRow(model.getRowCount() - 1);
		for (int i = 0; i < rows.size(); i++)
		{
			BleMacRow r = rows.get(i);
			if (i >= model.getRowCount())
				model.addRow(new Object[] { r.address, r.values() });
			else
			{
				if (!r.address.equals(model.getValueAt(i, 0)))
					model.setValueAt(r.address, i, 0);
				model.setValueAt(r.values(), i, 1);
			}
		}
		if (keepAddress != null)
		{
			for (int i = 0; i < rows.size(); i++)
			{
				if (keepAddress.equals(rows.get(i).address))
				{
					if (table.getSelectedRow() != i)
						table.setRowSelectionInterval(i, i);
					fireSelect();
					return;
				}
			}
		}
		if (table.getSelectedRow() >= rows.size())
			table.clearSelection();
		fireSelect();
	}

	private void fireSelect()
	{
		if (onSelect != null)
			onSelect.accept(selected());
	}
}
