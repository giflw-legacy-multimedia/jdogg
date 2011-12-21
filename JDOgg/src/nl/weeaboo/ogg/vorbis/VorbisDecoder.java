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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import nl.weeaboo.ogg.AbstractOggStreamHandler;
import nl.weeaboo.ogg.CircularBuffer;
import nl.weeaboo.ogg.OggCodec;
import nl.weeaboo.ogg.OggException;
import nl.weeaboo.ogg.CircularByteBuffer;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

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
	private CircularBuffer buffer;
	
	public VorbisDecoder() {
		super(OggCodec.Vorbis, false);

		info = new Info();
		comment = new Comment();
		dspState = new DspState();
		block = new Block(dspState);

		bufferStartFrame = bufferEndFrame = -1;

		temp = new byte[8192];
		buffer = new CircularByteBuffer(8 << 10, 32 << 20);
	}
	
	//Functions
	private void ensureTempSize(int bytes) {
		if (temp.length < bytes) {
			temp = new byte[Math.max(temp.length*2, bytes)];
		}
	}
	
	@Override
	public void clearBuffer() {
		packets.clear();
		buffer.clear();
		
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
			ensureTempSize(len);
			
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
			
			buffer.put(temp, 0, len);
			
			if (dspState.synthesis_read(samples) < 0) {
				break;
			}
		}
		
		bufferEndFrame = packet.granulepos;
		if (bufferStartFrame < 0) {
			bufferStartFrame = bufferEndFrame;
		}
	}
	
	@Override
	public byte[] read() throws IOException {
		ByteBuffer tempBuffer = ByteBuffer.wrap(temp);
		int r = read(tempBuffer);
		return Arrays.copyOfRange(temp, 0, r);
	}

	public int read(ByteBuffer out) throws OggException {
		if (!out.hasRemaining()) {
			return 0;
		}		
		return read0(out, out.remaining());
	}
	
	public int read(CircularByteBuffer out) throws OggException {
		return read0(out, out.getMaxCapacity() - out.size() - 1);
	}
	
	private int read0(Object out, int outLimit) throws OggException {
		if (!hasReadHeaders()) {
			throw new OggException("Haven't read headers yet");
		}
		
		while (!packets.isEmpty()) {
			Packet packet = packets.poll();
			processPacket(packet);
		}

		int frameSize = getFrameSize();
		int outFrames = Math.min(outLimit, buffer.size()) / frameSize;		
		int outBytes = outFrames * frameSize;
		
		if (out instanceof ByteBuffer) {
			ByteBuffer bout = (ByteBuffer)out;
			if (bout.hasArray()) {
				buffer.get(bout.array(), bout.arrayOffset()+bout.position(), outBytes);
				bout.position(bout.position()+outBytes);
			} else {
				ensureTempSize(outBytes);
				buffer.get(temp, 0, outBytes);
				bout.put(temp, 0, outBytes);
			}
		} else {
			CircularBuffer cout = (CircularBuffer)out;
			ensureTempSize(outBytes);
			
			buffer.get(temp, 0, outBytes);
			cout.put(temp, 0, outBytes);
		}
		
		if (buffer.size() == 0) {
			bufferStartFrame = bufferEndFrame;
		} else {
			bufferStartFrame += outFrames;
		}
		
		return outBytes;
	}

	@Override
	public boolean available() {
		return packets.size() > 0 || buffer.size() > 0;
	}

	@Override
	public boolean trySkipTo(double time) throws OggException {
		long targetFrame = (int)Math.floor(time * getFrameRate());
		while (bufferEndFrame < targetFrame && !packets.isEmpty()) {
			buffer.clear();
			bufferStartFrame = bufferEndFrame;
			
			Packet packet = packets.poll();
			processPacket(packet);
		}
		
		if (bufferEndFrame < targetFrame) {
			return false;
		}
		
		long skipFrames = Math.max(0, targetFrame - Math.max(0, bufferStartFrame));
		long skipBytes = skipFrames * getFrameSize();

		if (skipFrames < getFramesBuffered()) {
			buffer.skip((int)skipBytes);
			if (bufferStartFrame >= 0) {
				bufferStartFrame += skipFrames;
			}
		} else {
			buffer.clear();
			bufferStartFrame = bufferEndFrame;
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
		if (bufferStartFrame < 0 || bufferEndFrame < 0) {
			return 0;
		}
		return (int)(bufferEndFrame - bufferStartFrame);
	}
	
	//Setters
	
}
