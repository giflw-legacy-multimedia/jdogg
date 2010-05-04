package nl.weeaboo.jdogg.test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class OggAudioSink {

	private AudioFormat format;
	private float bytesPerSecond;
	
	private byte buffer[];
	private int bufferLength;
	private SourceDataLine line;
	private long written;
	
	public OggAudioSink(AudioFormat fmt) {
		format = fmt;
		bytesPerSecond = format.getFrameSize() * format.getFrameRate();
		
		buffer = new byte[8192];
	}
	
	//Functions
	public synchronized void start() throws LineUnavailableException {
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		if (line == null) {
			return;
		}

		line.open(format, Math.round(bytesPerSecond * 0.2f));
		line.start();
	}
	
	public synchronized void stop() {
		line.stop();
		line.flush();
		line.close();
	}
	
	public synchronized void buffer(byte b[], int off, int len) {
		while (buffer.length < bufferLength + len) {
			//Increase buffer size
			byte newBuffer[] = new byte[buffer.length * 2];
			System.arraycopy(buffer, 0, newBuffer, 0, bufferLength);
			buffer = newBuffer;
		}
		
		for (int n = 0; n < len; n++) {
			buffer[bufferLength + n] = b[off + n];
		}
		bufferLength += len;
	}
	
	public synchronized void reset() {
		bufferLength = 0;
		
		if (line != null) {
			line.flush();
			written = line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);
		}
	}
	
	public synchronized void flush() {
		if (line == null) {
			return;
		}		
		
		if (bufferLength < line.available()) {
			//System.err.print("[audio sink buffer underrun]"); 
		}
		
		int len = Math.min(line.available(), bufferLength);
		if (len == 0) {
			return;
		}
		
		int w = line.write(buffer, 0, len);
		if (w > 0) {
			for (int n = w; n < bufferLength; n++) {
				buffer[n - w] = buffer[n];
			}
			bufferLength -= w;
			written += w;
		}
	}
	
	//Getters
	public synchronized long getBufferLength() {
		long lineBytePos = line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);
		return bufferLength + (written - lineBytePos);
	}
	public synchronized double getBufferDuration() {
		return getBufferLength() / bytesPerSecond;
	}
	
	//Setters
}
