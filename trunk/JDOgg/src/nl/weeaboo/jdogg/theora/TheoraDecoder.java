package nl.weeaboo.jdogg.theora;

import java.util.ArrayDeque;
import java.util.Queue;

import nl.weeaboo.jdogg.AbstractOggStreamHandler;
import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggException;

import com.fluendo.jheora.Comment;
import com.fluendo.jheora.Info;
import com.fluendo.jheora.State;
import com.fluendo.jheora.YUVBuffer;
import com.jcraft.jogg.Packet;

public class TheoraDecoder extends AbstractOggStreamHandler {

    private Info info;
    private Comment comment;
    private State state;
    private VideoFormat videoFormat;
    private double packetTime;
    private double videoTime;
    private boolean unsynced;
    
	private Queue<VideoFrame> frames;
	
	public TheoraDecoder() {
		super(OggCodec.Theora, true);
		
		info = new Info();
		comment = new Comment();
		state = new State();
		
		frames = new ArrayDeque<VideoFrame>();
	}

	//Functions
	@Override
	public void flush() {
		super.flush();
		
		state.decodeInit(info);
		videoFormat = new VideoFormat(info.frame_width, info.frame_height,
				info.aspect_numerator, info.aspect_denominator,
				info.fps_numerator, info.fps_denominator);
		
		packetTime = -1;
		videoTime = -1;
		unsynced = true;
		
		frames.clear();
	}
		
	@Override
	protected void processHeader(Packet packet) throws OggException {
		if (info.decodeHeader(comment, packet) < 0) {
			throw new OggException("Error reading headers");
		}
	}

	@Override
	protected void processPacket(Packet packet) throws OggException {		
		if (unsynced) {
			if (!state.isKeyframe(packet)) {
				return;
			}
			unsynced = false;
		}

		processPacket0(packet);
		
		VideoFormat fmt = getVideoFormat();
		int w = fmt.getWidth();
		int h = fmt.getHeight();
		
		YUVBuffer yuvBuffer = new YUVBuffer();
		if (state.decodeYUVout(yuvBuffer) != 0) {
			throw new OggException("Error decoding YUV from Theora packet");
		}
		
		double frameDuration = videoFormat.getFrameDuration();		
		frames.add(new VideoFrame(yuvBuffer, w, h, packetTime, frameDuration));
	}
	
	private void processPacket0(Packet packet) {		
		if (state.decodePacketin(packet) != 0) {
			//throw new OggException("Error decoding Theora packet");
			return;
		}
								
		double t = getTime(packet);
		if (t >= 0) {
			packetTime = t;
		} else if (packetTime >= 0) {
			t = getDuration(packet);
			if (t >= 0) {
				packetTime += t;
			}
		}		
	}
	
	//Getters
	public void skip() {
		if (!frames.isEmpty()) {
			VideoFrame frame = frames.poll();
			if (frame != null) {
				videoTime = frame.getStartTime();
			}
		} else if (!packets.isEmpty()) {
			Packet packet = packets.poll();
			if (packet != null) {
				processPacket0(packet);
				videoTime = packetTime;
			}
		}		
	}
	
	public VideoFrame read() throws OggException {
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
		videoTime = frame.getStartTime();
		return frame;
	}
	
	@Override
	public double getTime() {
		return videoTime;
	}
	
	@Override
	public double getTime(Packet packet) {
		if (hasReadHeaders()) {
			return state.granuleTime(packet.granulepos);
		}
		return -1;
	}

	@Override
	public double getDuration(Packet packet) {
		return videoFormat.getFrameDuration();
	}
	
	@Override
	public boolean isBufferEmpty() {
		return packets.isEmpty() && frames.isEmpty();
	}
	
	public VideoFormat getVideoFormat() {
		if (!hasReadHeaders()) {
			throw new IllegalStateException("Headers not read yet!");
		}
		return videoFormat;
	}
	
	//Setters
	
}
