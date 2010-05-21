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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import nl.weeaboo.ogg.OggInput;
import nl.weeaboo.ogg.OggReader;
import nl.weeaboo.ogg.StreamUtil;
import nl.weeaboo.ogg.theora.TheoraDecoder;
import nl.weeaboo.ogg.theora.VideoFormat;
import nl.weeaboo.ogg.theora.VideoFrame;
import nl.weeaboo.ogg.vorbis.VorbisDecoder;

public class Player implements Runnable {

	private final double audioSync = 0.1;

	private Thread thread;
	private volatile boolean stop;
	private volatile boolean pauseState;
	private volatile boolean pauseRequest;
	private volatile double seekRequest;
	private volatile boolean ended;
	private boolean inputOk;
	
	private PlayerListener control;
	private VideoSink vsink;
	private AudioSink asink;

	//Only use within run() method
	private OggReader oggReader;
	private TheoraDecoder theorad;
	private VorbisDecoder vorbisd;
	//private KateDecoder kated;
		
	public Player(PlayerListener control, VideoSink vsink) {				
		this.control = control;
		this.vsink = vsink;
		
		oggReader = new OggReader();
	}
	
	//Functions
	public static void main(String args[]) throws IOException {
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}

		VideoWindow window = new VideoWindow("Theora/Vorbis Player");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Player player = new Player(window, window.getVideoPanel());
		player.setInput(StreamUtil.getOggInput(new File(args[0])));
		//player.setInput(StreamUtil.getOggInput("http://jvn.x10hosting.com/jdogg/small.ogv"));
		//player.setInput(StreamUtil.getOggInput("http://upload.wikimedia.org/wikipedia/commons/b/b5/I-15bis.ogg"));		
		player.start();		

		window.addVideoWindowListener(new VideoWindowListener() {
			public void onPause(boolean p) {
				player.setPaused(p);
			}
			public void onSeek(double frac) {
				player.seek(frac);
			}
			public void onSetInput(OggInput in) {
				try {
					player.setInput(in);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void start() {
		stop();
		
		synchronized (this) {
			stop = false;
			pauseState = false;
			ended = false;		
			seekRequest = -1;
			
			if (!inputOk) {
				throw new RuntimeException("Player input not set");
			}
			
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public void stop() {
		stop = true;		
		Thread t = null;
		
		synchronized (this) {
			t = thread;
			thread = null;
		}

		if (t != null) {
			t.interrupt();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void startAudio() {
		stopAudio();
		
		if (vorbisd == null) {
			return;
		}
		
		AudioFormat format = vorbisd.getAudioFormat();
		if (format == null) {
			return;
		}
		
		asink = new AudioSink(format);
		try {
			asink.start();
		} catch (LineUnavailableException lue) {
			lue.printStackTrace();
		} catch (InterruptedException e) {
			//Ignore
		}		
	}
	
	protected void stopAudio() {
		if (asink != null) {
			try {
				asink.stop();
			} catch (InterruptedException ie) {
				//Ignore
			}
			asink = null;
		}
	}
	
	public void run() {
		startAudio();
		
		try {
			//KateRendererState krs = new KateRendererState();
						
			double targetTime = -1;
			long lastTime = System.nanoTime();		
			while (!stop) {
				//Buffer data? We should probably limit the amount of data buffered.
				//if (!oggReader.isEOF()) {
				//	oggReader.read(true);
				//}
				
				//Process Theora
				while (targetTime < 0 || targetTime >= theorad.getTime()) {
					while (!oggReader.isEOF() && !theorad.available()) {
						oggReader.read();
					}
					if (!theorad.available()) break;
					
					if (targetTime >= 0) {
						//Skip frame if it's too late
						VideoFormat fmt = theorad.getVideoFormat();
						double vtime = theorad.getTime();
						double frameTime = fmt.getFrameDuration();
						if (vtime >= 0 && vtime + frameTime < targetTime) {
							theorad.skip();
							continue;
						}
					}
					
					VideoFrame frame = theorad.read();
					if (frame == null) continue;
					frame.readPixels(vsink);					
					if (targetTime < 0) break;
				}
				
				if (targetTime < 0) {
					targetTime = theorad.getTime();
					vorbisd.clearBuffer();
					if (asink != null) asink.reset();
				}
				
				//System.out.printf("T=%.2f V=%.2f A=%.2f DIFF=%.2f\n", targetTime, theorad.getTime(), asink.getTime(), theorad.getTime() - asink.getTime());			
				
				//Process Vorbis
				while (targetTime < 0 || targetTime >= vorbisd.getTime() - 0.50) {
					while (!oggReader.isEOF() && !vorbisd.available()) {
						oggReader.read();
					}
					
					if (!vorbisd.available()) {
						break;
					}
					
					byte bytes[] = vorbisd.read();
					double time = vorbisd.getTime();
					asink.buffer(bytes, time);
					
					if (targetTime < 0) {
						targetTime = asink.getTime();
					}
				}
							
				//Sync
				long curTime = System.nanoTime();
				if (targetTime >= 0) {
					double dt = (curTime - lastTime) / 1000000000.0;
					
					//Sync audio
					if (asink != null) {
						double atime = asink.getTime();
						double adiff = targetTime - atime;
						if (atime >= 0 && Math.abs(adiff) > audioSync
								&& asink.getBufferLength() > 0)
						{
							targetTime -= adiff * Math.min(1.0, 10 * dt);
						}
					}
					
					targetTime += dt;
				}
				lastTime = curTime;

				//Limit time
				double vwait = theorad.getTime() - targetTime;
				double await = (asink != null ? asink.getBufferDuration() : 0);
				double w = Math.min(vwait, await);
				if (w >= 0.001) {
					sleep(w);
				}
				
				//Go sleep for a bit if there's nothing more to do
				ended = oggReader.isEOF() && !theorad.available()
					&& !vorbisd.available();
				if (ended) {
					pauseRequest = true;
					sleep(0.1);
				}
				
				//Handle requests from Player
				synchronized (this) {
					while (pauseRequest && !stop) {
						pauseState = true;
						control.onPauseChanged(pauseState);
						if (asink != null) asink.reset();
						
						notify();						
						try {
							wait();
						} catch (InterruptedException ie) {
						}
					}
					
					if (pauseState) {
						lastTime = System.nanoTime();
						pauseState = false;
						control.onPauseChanged(pauseState);
						notifyAll();						
					}
					
					//Fuck Yeah Seeking
					if (seekRequest >= 0) {
						//if (!oggReader.isSeekSlow()) {
							oggReader.seekExact((theorad != null ? theorad : vorbisd),
									seekRequest);
						//} else {
						//	oggReader.seekApprox(seekRequest);
						//}
						lastTime = System.nanoTime();
						
						targetTime = -1;						
						seekRequest = -1;
					} else if (oggReader.isEOF()) {
						control.onTimeChanged(theorad.getEndTime(), theorad.getEndTime(), 1f);
					} else if (theorad.getTime() >= 0) {
						double time = theorad.getTime();
						double endTime = theorad.getEndTime();
						double frac = (endTime < 0 ? -1 : time / endTime);
						control.onTimeChanged(time, endTime, frac);
					}
				}
			}		
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (RuntimeException re) {
			re.printStackTrace();
		} finally {
			stopAudio();
			stop = true;
		}
	}
		
	public synchronized void seek(double s) {
		seekRequest = s;
	}
	
	protected void sleep(double time) {
		try {
			Thread.sleep(Math.round(1000.0 * time));
		} catch (InterruptedException ie) {
		}		
	}
	
	//Getters
	public synchronized int getWidth() {
		if (theorad == null) return 0;
		VideoFormat fmt = theorad.getVideoFormat();
		return fmt.getWidth();
	}
	public synchronized int getHeight() {
		if (theorad == null) return 0;
		VideoFormat fmt = theorad.getVideoFormat();
		return fmt.getHeight();
	}
	public synchronized double getFPS() {
		if (theorad == null) return 30;
		VideoFormat fmt = theorad.getVideoFormat();
		if (fmt.getFPSDenominator() == 0) {
			return 30;
		}
		return fmt.getFPSNumerator() / fmt.getFPSDenominator();
	}
	public boolean isPaused() {
		return pauseState;
	}
	public boolean isEnded() {
		return ended;
	}
	public synchronized boolean isSeekable() {
		return (oggReader != null && oggReader.isSeekable());
	}
	
	//Setters
	public void setInput(OggInput in) throws IOException {
		boolean wasStarted = !stop;
		
		stop();
		
		synchronized (this) {
			inputOk = false;			
			oggReader.setInput(in);
						
			//Setup stream handlers
			oggReader.addStreamHandler(theorad = new TheoraDecoder());
			oggReader.addStreamHandler(vorbisd = new VorbisDecoder());
			//oggReader.addStreamHandler(kated = new KateDecoder());
			
			//Read headers
			oggReader.readStreamHeaders();
			
			inputOk = true;
			
			if (wasStarted) {
				start();
			}
		}
	}
	
	public synchronized void setPaused(boolean p) {
		do {
			if (stop) {
				return;
			}
			
			pauseRequest = p;
			
			notify();
			
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		} while(pauseState != p);
	}

}
