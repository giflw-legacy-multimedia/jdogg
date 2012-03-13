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

import java.util.concurrent.ThreadFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import nl.weeaboo.ogg.CircularByteBuffer;
import nl.weeaboo.ogg.OggException;
import nl.weeaboo.ogg.vorbis.VorbisDecoder;

public class AudioSink {

	private Thread thread;
	private volatile boolean stop;
	
	private AudioFormat format;
	private float bytesPerSecond;
	private int bytesPerFrame;
	private ThreadFactory threadFactory;
	
	private CircularByteBuffer buffer;
	private SourceDataLine line;
	private long written;
	private double bufferEndTime;

	private byte[] temp;
	
	public AudioSink(AudioFormat fmt, ThreadFactory tfac) {
		format = fmt;
		bytesPerFrame = format.getFrameSize();
		bytesPerSecond = bytesPerFrame * format.getFrameRate();
		threadFactory = tfac;
		
		buffer = new CircularByteBuffer(16<<10, 256<<10);
	}
	
	//Functions
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
		buffer.put(b, off, len);		
		bufferEndTime = etime;
	}
		
	public synchronized void skipTime(double time) {
		int bytes = (int)Math.min(Integer.MAX_VALUE, Math.round(time * bytesPerSecond));
		
		buffer.skip(bytes);
	}
	
	public synchronized void reset() {
		buffer.clear();
		if (line != null) {
			line.flush();
			
			written = line.getLongFramePosition() * bytesPerFrame;
		}
	}
	
	public synchronized void flush() {
		if (line == null) {
			return;
		}		
		
		if (buffer.size() < line.available()) {
			//System.err.print("[audio sink buffer underrun]"); 
		}
		
		int len = Math.min(line.available(), buffer.size());
		if (len == 0) {
			return;
		}
		
		if (temp == null || temp.length < len) {
			temp = new byte[len];
		}
		buffer.get(temp, 0, len);
		
		int wr = 0;
		while (wr < len) {
			int w = line.write(temp, wr, len - wr);
			if (w < 0) break;
			wr += w;
			written += w;
		}
	}
	
	public synchronized void drain() {
		while (buffer.size() > 0) {
			flush();
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				//Ignore
			}
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
		return buffer.size() + lineBuffered;
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
					float minimum = volumeControl.getMinimum();
					float maximum = volumeControl.getMaximum();
		
					double db = Math.log10(vol) * 20; //Map linear volume to logarithmic dB scale
					
					volumeControl.setValue(Math.max(minimum, Math.min(maximum, (float)db)));
				}
			} catch (IllegalArgumentException iae) {
				throw new RuntimeException(iae);
			}
		}
	}
	
}
