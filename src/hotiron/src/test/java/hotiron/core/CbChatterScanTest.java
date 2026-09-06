package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CbChatterScanTest
{
	@Test
	void quietChannelAdvancesAfterDwell()
	{
		CbChatterScan scan = new CbChatterScan();
		scan.start(19, 0);
		assertEquals(CbChatterScan.Result.CONTINUE, scan.consider(19, false, 50));
		assertEquals(CbChatterScan.Result.CONTINUE, scan.consider(19, false, CbChatterScan.SETTLE_MS + 10));
		assertEquals(CbChatterScan.Result.NEXT,
				scan.consider(19, false, CbChatterScan.SETTLE_MS + CbChatterScan.DWELL_MS));
		assertEquals(20, scan.advance(1000));
	}

	@Test
	void chatterStopsTheScan()
	{
		CbChatterScan scan = new CbChatterScan();
		scan.start(5, 0);
		assertEquals(CbChatterScan.Result.CONTINUE, scan.consider(5, true, 10));
		assertEquals(CbChatterScan.Result.CONTINUE,
				scan.consider(5, true, CbChatterScan.SETTLE_MS + 10));
		assertEquals(CbChatterScan.Result.HIT,
				scan.consider(5, true, CbChatterScan.SETTLE_MS + CbChatterScan.HIT_MS + 10));
		assertFalse(scan.active());
		assertEquals(5, scan.current());
	}

	@Test
	void aPopDuringSettleDoesNotCount()
	{
		CbChatterScan scan = new CbChatterScan();
		scan.start(1, 0);
		assertEquals(CbChatterScan.Result.CONTINUE, scan.consider(1, true, 20));
		assertEquals(CbChatterScan.Result.NEXT,
				scan.consider(1, false, CbChatterScan.SETTLE_MS + CbChatterScan.DWELL_MS));
	}

	@Test
	void wrapsPastChannel40()
	{
		CbChatterScan scan = new CbChatterScan();
		scan.start(40, 0);
		assertEquals(1, scan.advance(10));
	}
}
