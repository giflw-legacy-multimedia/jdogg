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

package nl.weeaboo.ogg.test;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import nl.weeaboo.ogg.player.Player;

/*
 * TODO: Http input support
 * TODO: Kate subtitle support
 * TODO: Support for audio/subtitle only files
 * TODO: When seeking, there's a small pause due to the audio reading a bit
 * ahead. If I could read the audio to the correct point and then clear the
 * buffers that would be better.
 */
public class OggTest {

	//Functions
	public static void main(String args[]) throws IOException, LineUnavailableException, InterruptedException {
		/*
		long t0 = System.nanoTime();
		MemoryMXBean mb = ManagementFactory.getMemoryMXBean();
		long maxMem = 0;
		
		OggInput input = StreamUtil.getOggInput(new FileInputStream(args[0]));
		
		VorbisDecoder vorbisd = new VorbisDecoder();
		OggReader reader = new OggReader();
		reader.setInput(input);		
		reader.addStreamHandler(vorbisd);
		reader.readStreamHeaders();		
		
		double audioSync = 0.1;
		AudioSink asink = new AudioSink(vorbisd.getAudioFormat(), Executors.defaultThreadFactory());
		asink.start();

		long lastTime = System.nanoTime();
		double targetTime = 0;
		while (!reader.isEOF()) {
			maxMem = Math.max(maxMem, mb.getHeapMemoryUsage().getUsed());
			
			//Read vorbis data
			while (targetTime < 0 || targetTime > vorbisd.getTime() - audioSync) {
				if (!vorbisd.available()) {
					if (reader.isEOF()) {
						break;
					}
					reader.read();
				}
				asink.buffer(vorbisd);
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
						
			//Limit FPS
			System.out.printf("T=%.2f A=%.2f\n", targetTime, asink.getTime());			
			double w = (targetTime - asink.getTime());
			if (w > 0.01) {
				Thread.sleep(Math.round(0.5 * 1000.0 * w));
			}
		}
		input.close();
		asink.stop();
		
		System.out.printf("Memory Used: %.2fMB\n", maxMem / (double)(1<<20));
		System.out.printf("Time Spent: %.2fms\n", (System.nanoTime()-t0)/1000000.0);
		System.exit(1337);		
		*/
		
		Player.main(args);
	}
	
	//Getters
	
	//Setters
}
