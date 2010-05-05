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
	private volatile double seekRequest;
	
	private VideoWindow window;
	private AudioSink asink;

	//Only use within run() method
	private OggReader oggReader;
	private TheoraDecoder theorad;
	private VorbisDecoder vorbisd;
	//private KateDecoder kated;
		
	public Player() {		
		window = new VideoWindow("Theora/Vorbis/Kate Player");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.addVideoWindowListener(new VideoWindowListener() {
			public void onPause(boolean p) {
				setPaused(p);
			}
			public void onSeek(double frac) {
				seek(frac);
			}
		});
		
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

		Player player = new Player();
		player.setInput(StreamUtil.getOggInput(new File(args[0])));
		//player.setInput(new URL("http://jvn.x10hosting.com/jdogg/small.ogv"));
		//player.setInput(new URL("http://upload.wikimedia.org/wikipedia/commons/b/b5/I-15bis.ogg"));		
		player.start();		
	}
	
	public synchronized void start() {
		stop();
		
		stop = false;
		thread = new Thread(this);
		thread.start();
	}
	public synchronized void stop() {
		stop = true;
		
		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
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
			e.printStackTrace();
		}		
	}
	
	protected void stopAudio() {
		if (asink != null) {
			try {
				asink.stop();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			asink = null;
		}
	}
	
	public void run() {
		startAudio();
		
		try {
			//KateRendererState krs = new KateRendererState();
						
			double targetTime = 0;
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
					frame.readPixels(window.getVideoPanel());					
					if (targetTime < 0) break;
				}
				
				if (targetTime < 0) {
					targetTime = theorad.getTime();
					vorbisd.clearBuffer();
					asink.reset();
				}
				
				System.out.printf("T=%.2f V=%.2f A=%.2f DIFF=%.2f\n", targetTime, theorad.getTime(), asink.getTime(), theorad.getTime() - asink.getTime());			
				
				//Process Vorbis
				if (targetTime < 0 || targetTime >= vorbisd.getTime() - 0.5) {
					while (!oggReader.isEOF() && !vorbisd.available()) {
						oggReader.read();
					}
					if (vorbisd.available()) {
						byte bytes[] = vorbisd.read();
						double time = vorbisd.getTime();
						asink.buffer(bytes, time);
						
						if (targetTime < 0) {
							targetTime = asink.getTime();
						}
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
				if (oggReader.isEOF()) {
					pauseRequest = true;
					sleep(0.1);
				}
				
				//Handle requests from Player
				synchronized (this) {
					while (pauseRequest) {
						pauseState = true;
						asink.reset();
						window.setPaused(true);
						
						notify();						
						try {
							wait();
						} catch (InterruptedException ie) {
						}
					}
					
					if (pauseState) {
						lastTime = System.nanoTime();
						window.setPaused(false);

						pauseState = false;
						notifyAll();						
					}
					
					//Fuck Yeah Seeking
					if (seekRequest >= 0) {
						oggReader.seekFrac(seekRequest);
						lastTime = System.nanoTime();
						
						targetTime = -1;						
						seekRequest = -1;
					} else if (oggReader.isEOF()) {
						window.setPositionTime(theorad.getEndTime(), theorad.getEndTime());
						window.setPositionBytes(theorad.getEndTime(), theorad.getEndTime());
					} else if (theorad.getTime() >= 0) {
						window.setPositionTime(theorad.getTime(), theorad.getEndTime());
						window.setPositionBytes(theorad.getTime(), theorad.getEndTime());
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
	public boolean isPaused() {
		return pauseState;
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
	}
	
	public synchronized void setPaused(boolean p) {
		do {
			pauseRequest = p;
			
			notify();
			
			try {
				wait();
			} catch (InterruptedException ie) {
			}
		} while(pauseState != p);
	}

}
