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
	private float lineBufferDuration = 0.25f;
	
	private byte buffer[];
	private int bufferLength;
	private SourceDataLine line;
	private long written;
	private double bufferEndTime;
	
	public AudioSink(AudioFormat fmt) {
		format = fmt;
		bytesPerSecond = format.getFrameSize() * format.getFrameRate();
		
		buffer = new byte[8192];
	}
	
	//Functions
	public synchronized void start() throws LineUnavailableException, InterruptedException {
		stop();

		bufferEndTime = 0;		
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		
		line = (SourceDataLine) AudioSystem.getLine(info);
		if (line == null) {
			return;
		}

		line.open(format, Math.round(bytesPerSecond * lineBufferDuration));
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
	
	public void buffer(byte b[], double etime) {
		buffer(b, 0, b.length, etime);
	}
	public synchronized void buffer(byte b[], int off, int len, double etime) {
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
		
		bufferEndTime = etime;
	}
	
	public synchronized void skipBytes(int bytes) {
		advanceBytes(bytes);
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
		
			//written = line.getLongFramePosition() * (format.getSampleSizeInBits() >> 3);
			written += w;
		}		
	}
	
	//Getters
	public synchronized double getTime() {
		return bufferEndTime - getBufferDuration() - 2 * lineBufferDuration;
	}
	public synchronized long getBufferLength() {
		long lineBuffered = 0; //line.getBufferSize() - line.available();
		return bufferLength + lineBuffered;
	}
	public synchronized double getBufferDuration() {
		return getBufferLength() / bytesPerSecond;
	}
	
	//Setters
}
