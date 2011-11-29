/* JDOgg
 * 
 * Copyright (c) 2010 Timon Bijlsma
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package nl.weeaboo.ogg.player;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import nl.weeaboo.ogg.OggException;
import nl.weeaboo.ogg.vorbis.VorbisDecoder;

public class AudioSink {

	private Thread thread;
	private volatile boolean stop;
	
	private AudioFormat format;
	private float bytesPerSecond;
	private int bytesPerFrame;
	private ThreadFactory threadFactory;
	
	private ByteBuffer buffer;
	private SourceDataLine line;
	private long written;
	private double bufferEndTime;
	
	public AudioSink(AudioFormat fmt, ThreadFactory tfac) {
		format = fmt;
		bytesPerFrame = format.getFrameSize();
		bytesPerSecond = bytesPerFrame * format.getFrameRate();
		threadFactory = tfac;
		
		buffer = ByteBuffer.allocate(64 << 10);
	}
	
	//Functions
	private void resizeBuffer(int minCapacity) {
		int c = Math.max(buffer.capacity() * 2, minCapacity);
		ByteBuffer b = ByteBuffer.allocate(c);
		b.put(buffer);
		buffer = b;
	}
	
	public synchronized void start() throws LineUnavailableException, InterruptedException {
		start(10, .25f);
	}
	public synchronized void start(final long sleepTime, final double lineBufferDuration)
		throws LineUnavailableException, InterruptedException
	{	
		stop();

		bufferEndTime = 0;		
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		if (line == null) {
			return;
		}

		resizeBuffer((int)Math.round(bytesPerSecond * lineBufferDuration * 2));
		
		line.open(format, (int)Math.round(bytesPerSecond * lineBufferDuration));
		line.start();
		
		stop = false;
		thread = threadFactory.newThread(new Runnable() {
			public void run() {
				while (!stop) {
					flush();
					
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException ie) {						
					}
				}

				line.stop();
				line.flush();
				line.close();
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
	
	public synchronized void stop() throws InterruptedException {
		stop = true;
		if (thread != null) {
			thread.join();
		}
	}
	
	public synchronized int buffer(VorbisDecoder d) throws OggException {
		if (buffer.remaining() < d.getFrameSize()) {
			resizeBuffer(buffer.position() + d.getFrameSize());
		}		
		int r = d.read(buffer);
		if (r > 0) {
			bufferEndTime = d.getTime();
		}
		return r;
	}
	public void buffer(byte b[], double etime) {
		buffer(b, 0, b.length, etime);
	}
	public synchronized void buffer(byte b[], int off, int len, double etime) {
		if (buffer.remaining() < len) {
			resizeBuffer(buffer.position() + len);
		}

		buffer.put(b, off, len);		
		bufferEndTime = etime;
	}
	
	public synchronized void skipBytes(int bytes) {
		advanceBytes(bytes);
	}
	
	protected void advanceBytes(int skip) {
		//Skip regular-buffered bytes
		skip = Math.min(Math.max(0, skip), buffer.position());
		int newpos = buffer.position() - skip;
		for (int d = 0, s = skip; d < newpos; d++, s++) {
			buffer.put(d, buffer.get(s));
		}
		buffer.position(newpos);
	}
	
	public synchronized void skipTime(double time) {
		int bytes = (int)Math.min(Integer.MAX_VALUE, Math.round(time * bytesPerSecond));
		
		skipBytes(bytes);
	}
	
	public synchronized void reset() {
		buffer.rewind();
		if (line != null) {
			line.flush();
			
			written = line.getLongFramePosition() * bytesPerFrame;
		}
	}
	
	public synchronized void flush() {
		if (line == null) {
			return;
		}		
		
		if (buffer.position() < line.available()) {
			//System.err.print("[audio sink buffer underrun]"); 
		}
		
		int len = Math.min(line.available(), buffer.position());
		if (len == 0) {
			return;
		}
		
		int w = line.write(buffer.array(), buffer.arrayOffset(), len);
		if (w > 0) {
			advanceBytes(w);
		
			//written = line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);
			written += w;
		}		
	}
	
	//Getters
	public synchronized double getTime() {
		//double lbd = lineBufferDuration * 2;
		double lbd = (written - line.getLongFramePosition() * bytesPerFrame) / bytesPerSecond;
		return bufferEndTime - getBufferDuration() - lbd;
	}
	public synchronized long getBufferLength() {
		long lineBuffered = 0; //line.getBufferSize() - line.available();
		return buffer.position() + lineBuffered;
	}
	public synchronized double getBufferDuration() {
		return getBufferLength() / bytesPerSecond;
	}
	
	//Setters
	public synchronized void setVolume(double vol) {
		if (line != null && line.isOpen()) {
			try {
				FloatControl volumeControl = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
				if (vol == 0) {
					volumeControl.setValue(volumeControl.getMinimum());
				} else {					
					double minimum = volumeControl.getMinimum();
					double maximum = volumeControl.getMaximum();
					double xMin = Math.pow(10, minimum * .1);						
					double xMax = Math.pow(10, maximum * .1);
					double db = 10.0 * Math.log10(xMin + (vol * vol) * (xMax-xMin));
		
					volumeControl.setValue((float)db);
				}
			} catch (IllegalArgumentException iae) {
				throw new RuntimeException(iae);
			}
		}
	}
	
}
