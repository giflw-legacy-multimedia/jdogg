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

package nl.weeaboo.ogg.theora;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import com.fluendo.jheora.Comment;
import com.fluendo.jheora.Info;
import com.fluendo.jheora.State;
import com.fluendo.jheora.YUVBuffer;
import com.jcraft.jogg.Packet;

import nl.weeaboo.ogg.AbstractOggStreamHandler;
import nl.weeaboo.ogg.OggCodec;
import nl.weeaboo.ogg.OggException;

public class TheoraDecoder extends AbstractOggStreamHandler<VideoFrame> {

    private VideoFormat videoFormat;

    private Info info;
    private Comment comment;
    private State state;
	private boolean unsynced;
	
	private Queue<VideoFrame> frames;
	private double bufferStartTime, bufferEndTime;
	
	public TheoraDecoder() {
		super(OggCodec.Theora, true);
		
		info = new Info();
		comment = new Comment();
		state = new State();
		
		frames = new ArrayDeque<VideoFrame>();
		bufferStartTime = bufferEndTime = -1;
	}
	
	//Functions
	@Override
	public void clearBuffer() {
		packets.clear();
		frames.clear();		

		bufferStartTime = bufferEndTime;
	}	
	
	@Override
	public void reset() {
		clearBuffer();
		
		if (hasReadHeaders()) {
			onHeadersRead();
		}

		bufferStartTime = bufferEndTime = -1;
		unsynced = true;
	}
	
	@Override
	protected void onHeadersRead() {
		state.decodeInit(info);
		videoFormat = new VideoFormat(info.frame_width, info.frame_height,
				info.aspect_numerator, info.aspect_denominator,
				info.fps_numerator, info.fps_denominator);
	}

	@Override
	protected void processHeader(Packet packet) throws OggException {
		if (info.decodeHeader(comment, packet) < 0) {
			throw new OggException("Error reading headers");
		}
	}

	@Override
	protected void processPacket(Packet packet) throws OggException {
		if (!touchPacket(packet)) {
			return;
		}
		
		YUVBuffer yuvBuffer = new YUVBuffer();
		if (state.decodeYUVout(yuvBuffer) != 0) {
			throw new OggException("Error decoding YUV from Theora packet");
		}
		
		int w = videoFormat.getWidth();
		int h = videoFormat.getHeight();
		double frameDuration = videoFormat.getFrameDuration();
		frames.add(new VideoFrame(yuvBuffer, w, h, bufferEndTime - frameDuration,
				frameDuration));
	}
	
	protected boolean touchPacket(Packet packet) {
		if (unsynced) {
			if (!state.isKeyframe(packet)) {
				return false;
			}
			unsynced = false;
		}

		if (state.decodePacketin(packet) != 0) {
			//throw new OggException("Error decoding Theora packet");
			return false;
		}
				
		double frameDuration = videoFormat.getFrameDuration();		
		double t = state.granuleTime(packet.granulepos);		
		if (t >= 0) {
			bufferEndTime = t;
		} else if (bufferEndTime >= 0 && frameDuration >= 0) {
			bufferEndTime += frameDuration;
		} 		
		
		return true;
	}
	
	//Getters
	@Override
	public boolean isUnsynced() {
		return unsynced || getTime() < 0;
	}
	
	@Override
	public boolean available() {
		return packets.size() > 0 || frames.size() > 0;
	}

	@Override
	public VideoFrame read() throws IOException {
		if (!hasReadHeaders()) {
			throw new OggException("Haven't read headers yet");
		}

		while (frames.isEmpty() && !packets.isEmpty()) {
			Packet packet = packets.poll();
			processPacket(packet);
		}
		
		if (frames.isEmpty()) {
			return null;
		}
		
		VideoFrame frame = frames.poll();
		bufferStartTime = frame.getStartTime() + frame.getDuration();
		return frame;
	}

	public void skip() {
		if (!frames.isEmpty()) {
			frames.remove();
		} else if (!packets.isEmpty()) {
			Packet packet = packets.poll();
			if (packet != null) {
				touchPacket(packet);
			}
		}		
		
		if (hasReadHeaders()) {
			bufferStartTime += videoFormat.getFrameDuration();
		}
	}
	
	@Override
	public boolean trySkipTo(double time) throws OggException {
		while (available()) {
			double frameDuration = videoFormat.getFrameDuration();
			if (getTime() + frameDuration >= time) {
				return true;
			}
			skip();
		}
		return false;
	}
	
	@Override
	public double getTime() {
		return bufferStartTime;
	}

	@Override
	public double getEndTime() {
		if (hasReadHeaders() && stream != null) {
			return state.granuleTime(stream.getEndGranulePos());
		}
		return -1;
	}
		
	public VideoFormat getVideoFormat() {
		return videoFormat;
	}
	
	//Setters
	
}
