package nl.weeaboo.jdogg.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFrame;

import nl.weeaboo.jdogg.GreedyStreamSelector;
import nl.weeaboo.jdogg.OggDecoder;
import nl.weeaboo.jdogg.OggStreamHandler;
import nl.weeaboo.jdogg.kate.KateDecoder;
import nl.weeaboo.jdogg.kate.KateEvent;
import nl.weeaboo.jdogg.kate.KateRendererElement;
import nl.weeaboo.jdogg.kate.KateRendererState;
import nl.weeaboo.jdogg.theora.TheoraDecoder;
import nl.weeaboo.jdogg.theora.VideoFormat;
import nl.weeaboo.jdogg.theora.VideoFrame;
import nl.weeaboo.jdogg.vorbis.VorbisDecoder;

public class Player implements Runnable {

	private Thread thread;
	private volatile boolean stop;
	private volatile boolean pauseState;
	private volatile boolean pauseRequest;
	private volatile double seekRequest;
	
	private VideoWindow window;
	private AudioSink audioSink;

	//Only use within run() method
	private OggDecoder decoder;
	private TheoraDecoder theoraDecoder;
	private VorbisDecoder vorbisDecoder;
	private KateDecoder kateDecoder;
		
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
	}
	
	//Functions
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
		
		audioSink = new AudioSink(vorbisDecoder.getAudioFormat());
		try {
			audioSink.start();
		} catch (LineUnavailableException lue) {
			lue.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	protected void stopAudio() {
		if (audioSink != null) {
			try {
				audioSink.stop();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			audioSink = null;
		}
	}
	
	public void run() {
		startAudio();
		try {
			KateRendererState krs = new KateRendererState();
			
			boolean limitSpeed = true;
			double audioSyncFudge = 0.1;
			double audioSyncFactor = 10;
			
			double targetTime = 0;
			long lastTime = System.nanoTime();		
			while (!stop) {
				//Process Theora
				VideoFrame frame = null;
				while (targetTime < 0 || theoraDecoder.getTime() <= targetTime) {
					while (!decoder.isEOF() && theoraDecoder.isBufferEmpty()) {
						decoder.update();
					}
					
					if (targetTime >= 0) {
						double ttime = theoraDecoder.getTime();
						if (ttime >= targetTime) {
							break;
						}
						
						VideoFormat fmt = theoraDecoder.getVideoFormat();
						if (ttime >= 0 && ttime + fmt.getFrameDuration() < targetTime) {
							theoraDecoder.skip();
						} else {
							frame = theoraDecoder.read();
						}
					} else {
						frame = theoraDecoder.read();
						break;
					}
				}
				
				//Update video sink
				if (frame != null) {
					frame.readPixels(window.getVideoPanel());
				}
				
				//Process Vorbis
				while (vorbisDecoder.getTime() < 0.5 + Math.max(targetTime, theoraDecoder.getTime())) {
					while (!decoder.isEOF() && vorbisDecoder.isBufferEmpty()) {
						decoder.update();
					}
					
					byte b[] = vorbisDecoder.read();
					audioSink.buffer(b, 0, b.length);
				}

				//Process Kate
				while (!kateDecoder.isBufferEmpty()) {
					KateEvent event = kateDecoder.peek();
					if (event == null || event.getStartTime() >= decoder.getTime()) {
						break;
					}
					
					event = kateDecoder.read();
					krs.addElement(new KateRendererElement(event));
				}
						
				//Update GUI
				if (decoder.getTime() >= 0) {
					window.setPosition(decoder.getTime(), decoder.getEndTime());
				}

				//Update subtitle sink
				krs.update(decoder.getTime());
				window.setSubtitles(krs.getText(2));
							
				//Limit Speed
				long currentTime = System.nanoTime();
				if (targetTime >= 0) {
					double dt = (currentTime - lastTime) / 1000000000.0;
					
					//Sync audio
					double audioTime = audioTime();
					if (audioTime >= 0 && Math.abs(targetTime - audioTime) > audioSyncFudge) {
						double a = audioSyncFactor;
						double diff = (audioTime - targetTime);					
						targetTime += diff * Math.min(1.0, a * dt);
						
						//System.out.printf("%.2f %.2f %d\n", targetTime, audioTime, audioSink.getBufferLength());
					}
					
					//Increase targetTime
					targetTime += dt;
				}
				lastTime = currentTime;
				
				if (targetTime >= 0) {
					double wait = decoder.getTime() - targetTime;					
					if (limitSpeed && wait > 0) {
						try {
							Thread.sleep(Math.round(wait * 500.0));
						} catch (InterruptedException e) {							
						}
					}
				} else {
					//We're lost, find some timestamp to hang on to :)
					targetTime = decoder.getTime();
					skipAudio(targetTime);					
				}
				
				//Handle requests from Player
				synchronized (this) {
					while (pauseRequest) {
						pauseState = true;
						audioSink.reset();
						
						notify();						
						try {
							wait();
						} catch (InterruptedException ie) {
						}
					}
					
					if (pauseState) {
						targetTime = -1;

						pauseState = false;
						notifyAll();						
					}
					
					//Fuck Yeah Seeking
					if (seekRequest >= 0) {
						decoder.seekFrac(seekRequest);
						audioSink.reset();
						targetTime = -1;						
						seekRequest = -1;
					}
				}
			}		
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			stopAudio();
		}
	}
	
	protected void skipAudio(double targetTime) {
		//Skip audio to catch up with the video
		double audioSkip = targetTime - audioTime();
		if (targetTime >= 0 && audioSkip > 0) {
			audioSink.skipTime(audioSkip);
		}		
	}
	
	public synchronized void seek(double s) {
		seekRequest = s;
	}
	
	//Getters
	protected double audioTime() {
		return vorbisDecoder.getTime() - audioSink.getBufferDuration();		
	}
	
	public boolean isPaused() {
		return pauseState;
	}
	
	//Setters
	public synchronized void setInput(File file) throws IOException {
		setInput(new FileInputStream(file), file.length());
	}
	public synchronized void setInput(InputStream in, long length) throws IOException {
		decoder = new OggDecoder();
		decoder.setInput(in, length);
		
		//Scan through the entire file to detect all streams
		if (decoder.isSeekable()) {
			while (!decoder.isEOF()) {
				decoder.update();
			}
			decoder.seekFrac(0);
		}
		
		//Setup stream handlers
		theoraDecoder = new TheoraDecoder();
		vorbisDecoder = new VorbisDecoder();
		kateDecoder = new KateDecoder();
		
		OggStreamHandler streamHandlers[] = new OggStreamHandler[] {
				theoraDecoder, vorbisDecoder, kateDecoder
		};

		//Read the entire first stream group
		decoder.readStreamGroup(new GreedyStreamSelector(streamHandlers));		

		//Read the headers for the selected streams
		decoder.readHeaders();		
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
