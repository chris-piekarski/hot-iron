package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import hotiron.core.BandMark;
import hotiron.core.FrequencyAxis;

class QuickSelectBandLayerTest {

	@Test
	void fullSpanShowsFmAndWifiAndSkipsABandThatFillsTheView() {
		List<BandMark> wide = QuickSelectBandLayer.marks(FrequencyAxis.of(1, 7250, 2000));
		assertFalse(wide.isEmpty());
		boolean fm = false;
		boolean wifi = false;
		for (BandMark m : wide)
		{
			if ("FM".equals(m.label))
				fm = true;
			if ("WiFi 2".equals(m.label))
				wifi = true;
			assertTrue(m.fullHeightFill);
		}
		assertTrue(fm);
		assertTrue(wifi);
		assertTrue(QuickSelectBandLayer.marks(FrequencyAxis.of(88, 108, 400)).isEmpty(),
				"a single preset that fills the view must not paint a QS band");
	}

	@Test
	void surveyEnvelopesAreMarkedSurvey() {
		List<BandMark> marks = QuickSelectBandLayer.marks(FrequencyAxis.of(1, 7250, 900));
		boolean sawVhf = false;
		for (BandMark m : marks)
		{
			if ("VHF".equals(m.label))
			{
				sawVhf = true;
				assertEquals(BandMark.Style.SURVEY, m.style);
			}
			if ("FM".equals(m.label))
				assertEquals(BandMark.Style.PRIMARY, m.style);
		}
		assertTrue(sawVhf);
	}
}
