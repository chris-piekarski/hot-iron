package hotiron.core;

/**
 * One operator band face. Visibility rules live here so adding a tool
 * does not edit a boolean bag or a UI if/else chain. {@link BandContext}
 * shows the single best-fitting kind (Quick Select / parked), never a row
 * of faces.
 */
public enum BandToolKind
{
	FM
	{
		@Override
		boolean inView(FrequencyRange view)
		{
			if (view == null)
				return false;
			double start = view.getStartMHz();
			double end = view.getEndMHz();
			if (end <= start)
				return false;
			double overlap = BandContext.overlapMHz(start, end, FmChannelPlan.VIEW_START_MHZ,
					FmChannelPlan.VIEW_END_MHZ);
			if (overlap <= 0)
				return false;
			double span = end - start;
			if (span <= FM_SHOW_SPAN_MHZ)
				return true;
			return overlap / span >= FM_MIN_FRACTION;
		}

		@Override
		boolean hold(FrequencyRange view)
		{
			if (view == null)
				return false;
			double start = view.getStartMHz();
			double end = view.getEndMHz();
			return BandContext.overlapMHz(start, end, FmChannelPlan.VIEW_START_MHZ,
					FmChannelPlan.VIEW_END_MHZ) > 0 && (end - start) <= FM_HIDE_SPAN_MHZ;
		}

		@Override
		boolean pinned(boolean parked, ListenService service, boolean bleSniffing, BandScan scan)
		{
			return scan == BandScan.FM || (parked && service == ListenService.FM);
		}

		@Override
		double viewFit(FrequencyRange view)
		{
			if (view == null || !inView(view))
				return 0;
			return BandContext.overlapMHz(view.getStartMHz(), view.getEndMHz(), FmChannelPlan.VIEW_START_MHZ,
					FmChannelPlan.VIEW_END_MHZ) / Math.max(1, view.spanMHz());
		}
	},
	TV
	{
		@Override
		boolean inView(FrequencyRange view)
		{
			return view != null && TvBandLayer.tagsReadable(view.getStartMHz(), view.getEndMHz());
		}

		@Override
		boolean hold(FrequencyRange view)
		{
			if (view == null)
				return false;
			double start = view.getStartMHz();
			double end = view.getEndMHz();
			return TvChannelPlan.overlapsBroadcast(start, end) && (end - start) <= TV_HIDE_SPAN_MHZ;
		}

		@Override
		boolean pinned(boolean parked, ListenService service, boolean bleSniffing, BandScan scan)
		{
			return scan == BandScan.TV || (parked && service == ListenService.TV);
		}

		@Override
		double viewFit(FrequencyRange view)
		{
			if (view == null || !inView(view))
				return 0;
			int span = Math.max(1, view.spanMHz());
			double o = BandContext.overlapMHz(view.getStartMHz(), view.getEndMHz(), 54, 72)
					+ BandContext.overlapMHz(view.getStartMHz(), view.getEndMHz(), 76, 88)
					+ BandContext.overlapMHz(view.getStartMHz(), view.getEndMHz(), 174, 216)
					+ BandContext.overlapMHz(view.getStartMHz(), view.getEndMHz(), TvChannelPlan.UHF_VIEW_START_MHZ,
							TvChannelPlan.UHF_VIEW_END_MHZ);
			return o / span;
		}
	},
	NFC
	{
		@Override
		boolean inView(FrequencyRange view)
		{
			return view != null && NfcBandPlan.viewIsNfc(view.getStartMHz(), view.getEndMHz());
		}

		@Override
		boolean hold(FrequencyRange view)
		{
			if (view == null)
				return false;
			double start = view.getStartMHz();
			double end = view.getEndMHz();
			return NfcBandPlan.overlapsInterest(start, end) && (end - start) <= NFC_HIDE_SPAN_MHZ;
		}

		@Override
		boolean pinned(boolean parked, ListenService service, boolean bleSniffing, BandScan scan)
		{
			return scan == BandScan.NFC || (parked && service == ListenService.NFC);
		}
	},
	BLE
	{
		@Override
		boolean inView(FrequencyRange view)
		{
			/* BLE Quick Select (2400–2484), not Wi-Fi 2 (2402–2472). */
			return view != null && BleBandPlan.viewIsBle(view.getStartMHz(), view.getEndMHz());
		}

		@Override
		boolean hold(FrequencyRange view)
		{
			if (view == null)
				return false;
			double start = view.getStartMHz();
			double end = view.getEndMHz();
			return BleBandPlan.overlapsIsm(start, end) && (end - start) <= BLE_HIDE_SPAN_MHZ;
		}

		@Override
		boolean pinned(boolean parked, ListenService service, boolean bleSniffing, BandScan scan)
		{
			return bleSniffing;
		}
	};

	static final double FM_SHOW_SPAN_MHZ = 80;
	static final double FM_MIN_FRACTION = 0.10;
	static final double FM_HIDE_SPAN_MHZ = 200;
	static final double TV_HIDE_SPAN_MHZ = 220;
	static final double NFC_HIDE_SPAN_MHZ = 24;
	static final double BLE_HIDE_SPAN_MHZ = 180;

	abstract boolean inView(FrequencyRange view);

	abstract boolean hold(FrequencyRange view);

	abstract boolean pinned(boolean parked, ListenService service, boolean bleSniffing, BandScan scan);

	/**
	 * How much of {@code view} this kind owns (0–1). {@link BandContext}
	 * keeps the single highest fit so V-TV is TV, not FM+TV.
	 */
	double viewFit(FrequencyRange view)
	{
		if (view == null || !inView(view))
			return 0;
		return 1.0;
	}

	final boolean qualifies(FrequencyRange view, boolean parked, ListenService service, boolean bleSniffing,
			BandScan scan)
	{
		return pinned(parked, service, bleSniffing, scan) || inView(view);
	}
}
