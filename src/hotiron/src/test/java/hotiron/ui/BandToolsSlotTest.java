package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import hotiron.core.BandContext;
import hotiron.core.BandToolKind;

class BandToolsSlotTest
{
	@Test
	void idleWhenNothingQualifiesAndSizeIsLocked()
	{
		JLabel fm = new JLabel("fm");
		JLabel ble = new JLabel("ble");
		BandToolsSlot slot = new BandToolsSlot(new BandTool(BandToolKind.FM, fm),
				new BandTool(BandToolKind.BLE, ble));
		assertEquals(OperatorLayout.BAND_SLOT_HEIGHT, slot.getPreferredSize().height);
		assertEquals(OperatorLayout.TOOLS_WIDTH - 16, slot.getPreferredSize().width);
		assertFalse(slot.hosts(fm));
		assertFalse(slot.hosts(ble));
		assertEquals(BandContext.none(), slot.shown());
	}

	@Test
	void oneToolFillsTheSlot()
	{
		JLabel fm = new JLabel("fm");
		JLabel ble = new JLabel("ble");
		BandToolsSlot slot = new BandToolsSlot(new BandTool(BandToolKind.FM, fm),
				new BandTool(BandToolKind.BLE, ble));
		slot.apply(BandContext.of(BandToolKind.BLE));
		assertTrue(slot.hosts(ble));
		assertFalse(slot.hosts(fm));
		assertTrue(slot.getComponent(0) instanceof JScrollPane);
	}

	@Test
	void severalKindsStillShowOneFullWidthFace()
	{
		JLabel fm = new JLabel("fm");
		JLabel tv = new JLabel("tv");
		BandToolsSlot slot = new BandToolsSlot(new BandTool(BandToolKind.FM, fm),
				new BandTool(BandToolKind.TV, tv));
		int w = slot.getPreferredSize().width;
		int h = slot.getPreferredSize().height;
		slot.apply(BandContext.of(BandToolKind.FM, BandToolKind.TV));
		assertFalse(slot.hosts(fm), "V-TV / dual context shows TV, not FM beside it");
		assertTrue(slot.hosts(tv));
		assertTrue(slot.getComponent(0) instanceof JScrollPane);
		assertEquals(w, slot.getPreferredSize().width);
		assertEquals(h, slot.getPreferredSize().height);
	}

	@Test
	void applyingTheSameContextDoesNotRebuild()
	{
		JLabel fm = new JLabel("fm");
		BandToolsSlot slot = new BandToolsSlot(new BandTool(BandToolKind.FM, fm));
		slot.apply(BandContext.of(BandToolKind.FM));
		JPanel firstParent = (JPanel) fm.getParent();
		slot.apply(BandContext.of(BandToolKind.FM));
		assertSame(firstParent, fm.getParent());
	}

	@Test
	void unknownKindDoesNotShowAnUnregisteredFace()
	{
		JLabel fm = new JLabel("fm");
		BandToolsSlot slot = new BandToolsSlot(new BandTool(BandToolKind.FM, fm));
		slot.apply(BandContext.of(BandToolKind.NFC));
		assertFalse(slot.hosts(fm));
	}
}
