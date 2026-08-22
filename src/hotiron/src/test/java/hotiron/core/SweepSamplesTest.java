package hotiron.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SweepSamplesTest
{
	@Test
	void validCountsMapToHardwareBlocks()
	{
		assertEquals(1, SweepSamples.blocks(8192));
		assertEquals(2, SweepSamples.blocks(16384));
		assertEquals(32, SweepSamples.blocks(262144));
	}

	@Test
	void rejectsPartialOutOfRangeCounts()
	{
		assertThrows(IllegalArgumentException.class, () -> SweepSamples.requireValid(0));
		assertThrows(IllegalArgumentException.class, () -> SweepSamples.requireValid(10000));
		assertThrows(IllegalArgumentException.class, () -> SweepSamples.requireValid(270336));
	}
}
