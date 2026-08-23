package hotiron.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import hotiron.core.FrequencyRange;

/**
 * Quick Select + one sweep-range panel. Setting start and end separately
 * used to queue two USB restarts; presets apply as one range.
 */
public class FrequencySelectorRangeBinder
{
	public FrequencyRangePanel rangePanel;
	public QuickFrequencySelectorPanel selFreqQuick;
	private PropertyChangeListener rangeListener;
	private boolean applyingPreset;

	public FrequencySelectorRangeBinder(FrequencyRangePanel rangePanel, QuickFrequencySelectorPanel selFreqQuick)
	{
		this.rangePanel = rangePanel;
		this.selFreqQuick = selFreqQuick;
		VetoableChangeListener freqQuickVetoable = evt -> {
			QuickSelectPreset.findByLabel((String) evt.getNewValue()).ifPresent(preset ->
					applyPreset(preset.startMHz, preset.endMHz));
		};
		selFreqQuick.addVetoableChangeListener(freqQuickVetoable);
		rangePanel.addRangeListener(evt -> highlightCurrentRange());
		highlightCurrentRange();
	}

	public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		rangeListener = propertyChangeListener;
		rangePanel.addRangeListener(evt -> {
			if (!applyingPreset)
				propertyChangeListener.propertyChange(evt);
		});
	}

	/**
	 * Set start and end as one range so the sweep restarts once.
	 */
	void applyPreset(int startMHz, int endMHz) {
		applyingPreset = true;
		try {
			rangePanel.setRange(new FrequencyRange(startMHz, endMHz));
		} finally {
			applyingPreset = false;
		}
		highlightCurrentRange();
		if (rangeListener != null)
			rangeListener.propertyChange(new PropertyChangeEvent(this, "value", null, getFrequencyRange()));
	}

	private void highlightCurrentRange() {
		FrequencyRange range = rangePanel.getRange();
		selFreqQuick.highlightRange(range.getStartMHz(), range.getEndMHz());
	}

	public FrequencyRange getFrequencyRange() {
		return rangePanel.getRange();
	}
}
