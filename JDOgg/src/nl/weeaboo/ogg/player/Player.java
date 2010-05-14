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
	private volatile double seekRequestFrac, seekRequestTime;
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

		VideoWindow window = new VideoWindow("Theora/Vorbis/Kate Player");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final Player player = new Player(window, window.getVideoPanel());
		player.setInput(StreamUtil.getOggInput(new File(args[0])));
		//player.setInput(new URL("http://jvn.x10hosting.com/jdogg/small.ogv"));
		//player.setInput(new URL("http://upload.wikimedia.org/wikipedia/commons/b/b5/I-15bis.ogg"));		
		player.start();		

		window.addVideoWindowListener(new VideoWindowListener() {
			public void onPause(boolean p) {
				player.setPaused(p);
			}
			public void onSeek(double frac) {
				player.seekTime(frac);
			}
		});
	}
	
	public synchronized void start() {
		stop();
		
		stop = false;
		ended = false;
		seekRequestFrac = seekRequestTime = -1;
		
		if (!inputOk) {
			throw new RuntimeException("Player input not set");
		}
		
		thread = new Thread(this);
		thread.start();
	}
	public void stop() {
		stop = true;
		
		if (thread != null) {
			try {
				thread.interrupt();
				thread.join();
			} catch (InterruptedException ie) {
				//Ignore
			}
			thread = null;
		}
	}
	
	protected void startAudio() {
		stopAudio();
		
		asink = new AudioSink(vorbisd.getAudioFormat());
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
					asink.reset();
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
					double atime = asink.getTime();
					double adiff = targetTime - atime;
					if (atime >= 0 && Math.abs(adiff) > audioSync
							&& asink.getBufferLength() > 0)
					{
						targetTime -= adiff * Math.min(1.0, 10 * dt);
					}
					
					targetTime += dt;
				}
				lastTime = curTime;

				//Limit time
				double vwait = theorad.getTime() - targetTime;
				double await = asink.getBufferDuration();
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
						asink.reset();
						
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
					if (seekRequestFrac >= 0 || seekRequestTime >= 0) {
						if (seekRequestTime >= 0) {
							oggReader.seekExact((theorad != null ? theorad : vorbisd),
									seekRequestTime);
						} else {
							oggReader.seekApprox(seekRequestFrac);
						}
						lastTime = System.nanoTime();
						
						targetTime = -1;						
						seekRequestFrac = seekRequestTime = -1;
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
		}
	}
		
	public synchronized void seekFrac(double s) {
		seekRequestFrac = s;
	}
	public synchronized void seekTime(double t) {
		seekRequestTime = t;
	}
	
	protected void sleep(double time) {
		try {
			Thread.sleep(Math.round(1000.0 * time));
		} catch (InterruptedException ie) {
		}		
	}
	
	//Getters
	public int getWidth() {
		if (theorad == null) return 0;
		VideoFormat fmt = theorad.getVideoFormat();
		return fmt.getWidth();
	}
	public int getHeight() {
		if (theorad == null) return 0;
		VideoFormat fmt = theorad.getVideoFormat();
		return fmt.getHeight();
	}
	public double getFPS() {
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
	
	//Setters
	public synchronized void setInput(OggInput in) throws IOException {
		oggReader.setInput(in);
				
		//Setup stream handlers
		oggReader.addStreamHandler(theorad = new TheoraDecoder());
		oggReader.addStreamHandler(vorbisd = new VorbisDecoder());
		//oggReader.addStreamHandler(kated = new KateDecoder());

		//Read headers
		oggReader.readStreamHeaders();
		
		inputOk = true;
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
