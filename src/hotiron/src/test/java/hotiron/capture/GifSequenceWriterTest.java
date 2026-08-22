package hotiron.capture;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.junit.jupiter.api.Test;

class GifSequenceWriterTest {

    @Test
    void testWriteSequenceAndClose() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(baos);

        GifSequenceWriter writer = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_RGB, 100, true);

        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        writer.writeToSequence(img);

        writer.close();
        ios.close();

        byte[] data = baos.toByteArray();
        assertTrue(data.length > 0);
        // GIF header check roughly
        assertEquals('G', data[0]);
        assertEquals('I', data[1]);
        assertEquals('F', data[2]);
    }
}
