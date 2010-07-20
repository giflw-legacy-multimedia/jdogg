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

package nl.weeaboo.ogg.vorbis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import nl.weeaboo.ogg.AbstractOggStreamHandler;
import nl.weeaboo.ogg.OggCodec;
import nl.weeaboo.ogg.OggException;

public class VorbisDecoder extends AbstractOggStreamHandler<byte[]> {

	private AudioFormat audioFormat;

	private Info info;
	private Comment comment;
	private DspState dspState;
	private Block block;
	
	private float pcm[][][];
	private int index[];
	private long bufferStartFrame, bufferEndFrame;
	
	private byte[] temp;
	private ByteArrayOutputStream bout;
	
	public VorbisDecoder() {
		super(OggCodec.Vorbis, true);

		info = new Info();
		comment = new Comment();
		dspState = new DspState();
		block = new Block(dspState);

		bufferStartFrame = bufferEndFrame = -1;

		temp = new byte[4096];
		bout = new ByteArrayOutputStream();
	}
	
	//Functions
	@Override
	public void clearBuffer() {
		packets.clear();
		bout.reset();
		
		bufferStartFrame = bufferEndFrame;
	}
	
	@Override
	public void reset() {
		clearBuffer();
		
		if (hasReadHeaders()) {
			onHeadersRead();
		}
		
		bufferStartFrame = bufferEndFrame = -1;
	}

	@Override
	protected void onHeadersRead() {
		dspState.synthesis_init(info);
		block.init(dspState);
		pcm = new float[1][][];
		index = new int[info.channels];
		
		audioFormat = new AudioFormat(info.rate, 16, info.channels, true, false);
	}
	
	@Override
	protected void processHeader(Packet packet) throws OggException {
		if (info.synthesis_headerin(comment, packet) < 0) {
			throw new OggException("Error reading headers");
		}
	}

	@Override
	protected void processPacket(Packet packet) {
		if (packet.packetno < 3) {
			return;
		}
		
		int res = block.synthesis(packet);
		if (res == 0) {
			dspState.synthesis_blockin(block);
		} else {
			return;
		}
		
		int frameSize = getFrameSize();
		int ptrinc = frameSize;
		
		int samples;
		while ((samples = dspState.synthesis_pcmout(pcm, index)) > 0) {
			float[][] p = pcm[0];
			int len = samples * frameSize;
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
		
		bufferEndFrame = packet.granulepos;
	}
	
	@Override
	public byte[] read() throws IOException {
		return read((int)(bufferEndFrame - bufferStartFrame));
	}
	
	public byte[] read(int frames) throws OggException {
		if (frames > bufferEndFrame - bufferStartFrame) {
			throw new IllegalArgumentException(String.format("Can't read %d frames, only %d buffered.", frames, bufferEndFrame-bufferStartFrame));
		}
		
		if (!hasReadHeaders()) {
			throw new OggException("Haven't read headers yet");
		}
		
		while (!packets.isEmpty()) {
			Packet packet = packets.poll();
			processPacket(packet);
		}
		
		byte result[] = bout.toByteArray();
		bout.reset();		

		int frameSize = getFrameSize();
		int offset = frames * frameSize;
		if (offset <= 0 || offset >= result.length) {
			bufferStartFrame = bufferEndFrame;
			return result;
		} else {			
			bufferStartFrame += frames;
			bout.write(result, offset, result.length - offset);
			return Arrays.copyOf(result, offset);
		}
	}

	@Override
	public boolean available() {
		return packets.size() > 0 || bout.size() > 0;
	}

	@Override
	public boolean trySkipTo(double time) throws OggException {
		if (bufferEndFrame < time) {
			bout.reset();
			return false;
		}
		
		double skipTime = bufferEndFrame - time;
		int skipBytes = (int)Math.round(skipTime * getFrameRate() * getFrameSize());

		if (skipBytes > 0) {			
			if (skipBytes < bout.size()) {
				byte bytes[] = bout.toByteArray();
				bout.reset();
				bout.write(bytes, skipBytes, bytes.length - skipBytes);
			} else {
				bout.reset();
			}
		}
		
		return true;
	}
	
	//Getters
	@Override
	public boolean isUnsynced() {
		return getTime() < 0;
	}
	
	@Override
	public double getTime() {
		if (bufferStartFrame >= 0) {
			return bufferStartFrame / getFrameRate();
		}
		return -1;
	}

	@Override
	public double getEndTime() {
		if (hasReadHeaders() && stream != null && stream.getEndGranulePos() >= 0) {
			return stream.getEndGranulePos() / (double)info.rate;
		}
		return -1;
	}
	
	public AudioFormat getAudioFormat() {
		return audioFormat;
	}
	public double getFrameRate() {
		if (hasReadHeaders()) {
			return audioFormat.getFrameRate();
		}
		return 44100;
	}
	public int getFrameSize() {
		if (hasReadHeaders()) {
			return audioFormat.getFrameSize();
		}
		return getChannels() * ((getSampleSizeInBits()+7) / 8);
	}
	public int getChannels() {
		if (hasReadHeaders()) {
			return audioFormat.getChannels();
		}
		return 2;
	}
	public int getSampleSizeInBits() {
		if (hasReadHeaders()) {
			return audioFormat.getSampleSizeInBits();
		}
		return 16;
	}
	public int getFramesBuffered() {
		return (int)(bufferEndFrame - bufferStartFrame);
	}
	
	//Setters
	
}
