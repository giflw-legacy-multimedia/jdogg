package nl.weeaboo.jdogg;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;

public class OggStream {

	private int id;
	private StreamState state;
	private OggCodec codec;
	private OggStreamHandler handler;
	private Packet beginPacket;	
	private Packet endPacket;	
	private double endTime;
	
	private Packet packet = new Packet(); //Reused Packet object
	
	public OggStream(int id) {
		this.id = id;
		
		state = new StreamState();
		state.init(id);
		
		endTime = -1;
	}
	
	//Functions
	public void flush() {
		state.reset();

		if (handler != null) {
			handler.flush();
		}
	}
	
	public void process(Page page) throws OggException {
		int res = state.pagein(page);
		if (res < 0) {
			throw new OggException(String.format("Error in StreamState.pagein() :: %d", res));
		}
		
		while (true) {
			res = state.packetout(packet);
			if (res < 0) {
				throw new OggException(String.format("Error in StreamState.packetout() :: %d", res));
			} else if (res == 0) {
				break; //Needs more data
			}

			//Process packet
			if (codec == null) {
				codec = OggCodec.fromSignature(packet.packet_base,
							packet.packet+1, packet.bytes-1);				
			}
			
			if (packet.b_o_s != 0) {
				beginPacket = StreamUtil.clone(packet);
			}
			if (packet.e_o_s != 0) {
				endPacket = StreamUtil.clone(packet);
			}

			if (handler != null) {
				handler.process(packet);
			}
		}		
	}
	
	@Override
	public String toString() {
		return String.format("%s[codec=%s,id=%d]", getClass().getName(),
				String.valueOf(codec), id);
	}
	
	//Getters
	public int getId() {
		return id;
	}	
	public OggCodec getCodec() {
		return codec;
	}
	public OggStreamHandler getHandler() {
		return handler;
	}
	public double getTime() {
		if (handler != null) {
			return handler.getTime();
		}
		return -1;
	}
	public double getEndTime() {
		if (endTime < 0 && handler != null && endPacket != null) {
			double t = handler.getTime(endPacket);
			endTime = Math.max(endTime, t);
		}
		return endTime;
	}
	
	//Setters
	public void setHandler(OggStreamHandler h) {
		handler = h;
		
		if (handler != null) {
			handler.init(this);
			
			try {
				if (beginPacket != null) {
					handler.process(beginPacket);
				}
			} catch (OggException ogge) {				
			}
		}
	}
	
}
