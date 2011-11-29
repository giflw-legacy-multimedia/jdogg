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

package nl.weeaboo.ogg;

import java.io.IOException;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;

public class OggStream {

	private int id;
	private StreamState streamState;
	private OggStreamHandler<?> handler;
	private boolean error;
	private boolean needsMoreData;
	private boolean eos;
	
	private OggCodec codec;
	private Packet beginPacket;
	private boolean seenDataPacket;
	private long granulePos;
	private long endGranulePos;
	
	public OggStream(int id) {
		this.id = id;
		
		streamState = new StreamState();
		streamState.init(id);
		
		reset0();
		endGranulePos = 0;
	}
	
	//Functions
	public void reset() {
		reset0();

		if (handler != null) {
			handler.reset();
		}
	}
	
	private void reset0() {
		streamState.reset();
		error = false;
		needsMoreData = true;
		eos = false;
		
		seenDataPacket = false;
		granulePos = -1;		
	}
		
	public boolean read(Packet packet) throws OggException {
		int res = streamState.packetout(packet);
		if (res < 0) {
			error = true;
			//throw new OggException(String.format("Error in StreamState.packetout() :: %d", res));
			return false;
		} else if (res == 0) {
			needsMoreData = true;
			return false;
		}

		//Process packet
		if (packet.b_o_s == 0 && packet.e_o_s == 0) {
			seenDataPacket = true;
		} else {
			if (packet.b_o_s != 0) {
				beginPacket = StreamUtil.clone(packet);
				codec = OggCodec.fromSignature(packet.packet_base, packet.packet, packet.bytes);
			}
			
			if (packet.e_o_s != 0) {
				endGranulePos = packet.granulepos;
				eos = true;
			}
		}
				
		if (packet.granulepos >= 0) {
			granulePos = packet.granulepos;
		}
		
		if (handler != null) {
			handler.process(packet);
		}
		
		return true;
	}
	
	//Getters
	public int getId() {
		return id;
	}
	public OggStreamHandler<?> getHandler() {
		return handler;
	}
	public OggCodec getCodec() {
		return codec;
	}
	public Packet getBeginPacket() {
		return beginPacket;
	}
	public boolean isError() {
		return error;
	}
	public boolean needsMoreData() {
		return needsMoreData;
	}
	public boolean isEOS() {
		return eos;
	}
	public long getGranulePos() {
		return granulePos;
	}
	public long getEndGranulePos() {
		return endGranulePos;
	}
	public boolean hasSeenDataPacket() {
		return seenDataPacket;
	}
	
	//Setters
	public void setInput(Page page) throws IOException {		
		if (!needsMoreData()) {
			throw new IOException("Doesn't need any input");
		}
		
		int res = streamState.pagein(page);
		if (res < 0) {
			throw new OggException(String.format("Error in StreamState.pagein() :: %d", res));
		}
	}
	
	public void setHandler(OggStreamHandler<?> h) {
		handler = h;
		if (handler != null) {
			handler.setStream(this);
		}
	}
	
	public void setEndGranulePos(long pos) {
		endGranulePos = pos;
	}
	
}
