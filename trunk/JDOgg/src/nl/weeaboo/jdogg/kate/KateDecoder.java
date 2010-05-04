package nl.weeaboo.jdogg.kate;

import java.util.ArrayDeque;
import java.util.Queue;

import nl.weeaboo.jdogg.AbstractOggStreamHandler;
import nl.weeaboo.jdogg.OggCodec;
import nl.weeaboo.jdogg.OggException;

import com.fluendo.jkate.Comment;
import com.fluendo.jkate.Event;
import com.fluendo.jkate.Info;
import com.fluendo.jkate.State;
import com.jcraft.jogg.Packet;

public class KateDecoder extends AbstractOggStreamHandler {

    private Info info;
    private Comment comment;
    private State state;
    private double eventTime;
    
    private Queue<KateEvent> events;
	
	public KateDecoder() {
		super(OggCodec.Kate, false);

		info = new Info();
		comment = new Comment();
		state = new State();
		
		events = new ArrayDeque<KateEvent>();
	}

	//Functions
	@Override
	public void flush() {
		super.flush();
		
		state.decodeInit(info);
		events.clear();
	}
	
	@Override
	protected void processHeader(Packet packet) throws OggException {
		if (info.decodeHeader(comment, packet) < 0) {
			throw new OggException("Error reading headers");
		}
	}

	@Override
	protected void processPacket(Packet packet) throws OggException {
		KateEvent event = convertPacket(packet);
		if (event != null) {
			events.add(event);
		}
	}
	
	protected KateEvent convertPacket(Packet packet) throws OggException {
		if (packet.e_o_s != 0) {
			return null;
		}
		
		if (state.decodePacketin(packet) != 0) {
			throw new OggException("Error decoding Kate packet");
		}
		
		Event event = state.decodeEventOut();
		if (event == null) {
			return null;
		}
		
		return new KateEvent(event);
	}

	//Getters
	public KateEvent read() throws OggException {
		peek();
		
		KateEvent event = events.poll();
		eventTime = event.getStartTime();
		return event;
	}
	
	public KateEvent peek() throws OggException {
		if (!hasReadHeaders()) {
			throw new OggException("Haven't read headers yet");
		}

		while (events.isEmpty() && !packets.isEmpty()) {
			Packet packet = packets.poll();
			processPacket(packet);
		}

		return events.peek();
	}
	
	@Override
	public boolean isBufferEmpty() {
		return packets.isEmpty() && events.isEmpty();
	}

	@Override
	public double getTime() {
		return eventTime;
	}
	
	@Override
	public double getTime(Packet packet) {
		try {
			KateEvent event = convertPacket(packet);
			if (event != null) {
				return event.getStartTime();
			}
		} catch (OggException e) {
		}
		return -1;
	}

	@Override
	public double getDuration(Packet packet) {
		try {
			KateEvent event = convertPacket(packet);
			if (event != null) {
				double start = event.getStartTime();
				double end = event.getEndTime();
				if (start >= 0 && end >= start) {
					return end - start;
				}
			}
		} catch (OggException e) {
		}
		return -1;
	}
	
	//Setters
	
}
