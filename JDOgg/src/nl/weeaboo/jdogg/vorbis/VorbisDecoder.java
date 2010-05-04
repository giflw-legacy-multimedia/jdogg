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

package nl.weeaboo.jdogg.vorbis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import nl.weeaboo.jdogg.AbstractOggStreamHandler;
import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggException;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class VorbisDecoder extends AbstractOggStreamHandler {

	private Info info;
	private Comment comment;
	private DspState dspState;
	private Block block;
	private float pcm[][][];
	private int index[];
	private long currentSample;
	
	private byte[] temp;
	private ByteArrayOutputStream bout;
	
	public VorbisDecoder() {
		super(OggCodec.Vorbis, false);
		
		info = new Info();
		comment = new Comment();
		dspState = new DspState();
		block = new Block(dspState);

		temp = new byte[8192];
		bout = new ByteArrayOutputStream();
	}
	
	//Functions
	@Override
	public void flush() {
		super.flush();
		
		dspState.synthesis_init(info);
		block.init(dspState);
		pcm = new float[1][][];
		index = new int[info.channels];

		currentSample = -1;		
		bout.reset();
	}
	
	@Override
	protected void processHeader(Packet packet) throws OggException {		
		if (info.synthesis_headerin(comment, packet) < 0) {
			throw new OggException("Error reading headers");
		}
	}
	
	@Override
	protected void processPacket(Packet packet) throws OggException {
		if (currentSample < 0) {
			//Skip packets until we are sure where we are in the stream again
			if (packet.granulepos < 0) {
				return;
			}
			
			currentSample = packet.granulepos;
		}
		
		if (packet.packetno < 3) {
			return;
		}

		int res = block.synthesis(packet);
		if (res == 0) {
			dspState.synthesis_blockin(block);
		} else {
			return;
		}
				
		int bps = getBytesPerSample();
		int ptrinc = info.channels * bps;
		
		int samples;
		while ((samples = dspState.synthesis_pcmout(pcm, index)) > 0) {
			float[][] p = pcm[0];
			int len = samples * info.channels * bps;
			if (temp.length < len) {
				temp = new byte[len];
			}

			for (int ch = 0; ch < info.channels; ch++) {
				int ptr = (ch << 1);
				for (int j = 0; j < samples; j++) {
					int val = (int)(p[ch][index[ch] + j] * 32767f);
					
					if (val > Short.MAX_VALUE) {
						val = Short.MAX_VALUE;
					} else if (val < Short.MIN_VALUE) {
						val = Short.MIN_VALUE;
					}
					
					temp[ptr    ] = (byte)(val & 0xFF);
					temp[ptr + 1] = (byte)((val >> 8) & 0xFF);
					ptr += ptrinc;
				}
			}
			
			bout.write(temp, 0, len);
			
			if (dspState.synthesis_read(samples) < 0) {
				break;
			}
		}
	}
	
	//Getters
	@Override
	public boolean isBufferEmpty() {
		return packets.isEmpty() && bout.size() == 0;
	}
	
	@Override
	public double getTime() {
		if (currentSample < 0 || !hasReadHeaders()) {
			return -1;
		}
		return currentSample / (double)info.rate;
	}
	
	@Override
	public double getTime(Packet packet) {
		if (hasReadHeaders() && packet.granulepos >= 0) {
			return packet.granulepos / (double)info.rate;
		}
		return -1;
	}
	
	@Override
	public double getEndTime() {
		if (!hasReadHeaders()) {
			return super.getEndTime();
		}
		return info.rate;
	}
	
	@Override
	public double getDuration(Packet packet) {
		return -1; //Variable bitrate, we don't know without decoding
	}
	
	public int getBytesPerSample() {
		return 2;
	}
	
	public int getChannels() {
		return (info != null ? info.channels : 2);
	}

	public AudioFormat getAudioFormat() {
		if (!hasReadHeaders()) {
			throw new IllegalStateException("Headers not read yet!");
		}
		return new AudioFormat(info.rate, getBytesPerSample() * 8,
				getChannels(), true, false);
	}
	
	public byte[] read() throws IOException {
		if (!hasReadHeaders()) {
			throw new OggException("Haven't read headers yet");
		}
		
		while (!packets.isEmpty()) {
			Packet packet = packets.poll();
			processPacket(packet);
		}
		
		byte result[] = bout.toByteArray();
		
		currentSample += result.length / (getChannels() * getBytesPerSample());
		bout.reset();
		
		return result;
	}
	
	//Setters
	
}
