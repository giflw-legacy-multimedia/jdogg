package nl.weeaboo.jdogg.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioSink {

	private Thread thread;
	private volatile boolean stop;
	
	private AudioFormat format;
	private float bytesPerSecond;
	
	private byte buffer[];
	private int bufferLength;
	private SourceDataLine line;
	private long written;
	
	public AudioSink(AudioFormat fmt) {
		format = fmt;
		bytesPerSecond = format.getFrameSize() * format.getFrameRate();
		
		buffer = new byte[8192];
	}
	
	//Functions
	public synchronized void start() throws LineUnavailableException, InterruptedException {
		stop();
				
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		if (line == null) {
			return;
		}

		line.open(format, Math.round(bytesPerSecond * 0.2f));
		line.start();
		
		stop = false;
		thread = new Thread(new Runnable() {
			public void run() {
				while (!stop) {
					flush();
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {						
					}
				}

				line.stop();
				line.flush();
				line.close();
			}
		});
		thread.start();
	}
	
	public synchronized void stop() throws InterruptedException {
		stop = true;
		if (thread != null) {
			thread.join();
		}
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
	
	public synchronized void skipBytes(int bytes) {
		long buffered = written - getLineBytePos();
		
		//Skip line-buffered bytes
		if (bytes < buffered) {
			if (line != null) {
				line.flush();
				written = line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);
			}
			return;
		} else {
			bytes -= buffered;
		}
		
		advanceBytes(bufferLength);
	}
	
	protected void advanceBytes(int bytes) {
		//Skip regular-buffered bytes
		bytes = Math.min(Math.max(0, bytes), bufferLength);		
		for (int n = bytes; n < bufferLength; n++) {
			buffer[n - bytes] = buffer[n];
		}
		bufferLength -= bytes;		
	}
	
	public synchronized void skipTime(double time) {
		int bytes = (int)Math.min(Integer.MAX_VALUE, Math.round(time * bytesPerSecond));
		
		skipBytes(bytes);
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
			advanceBytes(w);
			written += w;
		}
	}
	
	//Getters
	protected synchronized long getLineBytePos() {
		return line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);		
	}
	public synchronized long getBufferLength() {
		return bufferLength + (written - getLineBytePos());
	}
	public synchronized double getBufferDuration() {
		return getBufferLength() / bytesPerSecond;
	}
	
	//Setters
}
