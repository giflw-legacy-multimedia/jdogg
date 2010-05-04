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

package nl.weeaboo.jdogg;

import java.util.ArrayDeque;
import java.util.Queue;

import com.jcraft.jogg.Packet;

public abstract class AbstractOggStreamHandler implements OggStreamHandler {

	private boolean deferredHandling;
	
	private OggStream stream;
	private OggCodec codec;
	private boolean hasReadHeaders;
	
	protected Queue<Packet> packets;
	
	public AbstractOggStreamHandler(OggCodec c, boolean defer) {
		deferredHandling = defer;

		codec = c;
		
		packets = new ArrayDeque<Packet>();
	}
	
	//Functions
	public void flush() {
		packets.clear();
	}
	
	@Override
	public void setStream(OggStream s) {
		stream = s;
		hasReadHeaders = false;
		
		flush();
	}
	
	@Override
	public void process(Packet packet) throws OggException {
		if (!hasReadHeaders()) {
			processHeader(packet);
			
			if (packet.packetno >= 2) {
				readHeaders();
			}
		} else {
			if (packet.packetno <= 2) {
				//Header packet
				return;
			}
			
			if (deferredHandling) {
				packets.add(StreamUtil.clone(packet));
			} else {
				processPacket(packet);
			}
		}
	}
	
	protected void readHeaders() {
		if (!hasReadHeaders) {
			hasReadHeaders = true;
			flush();
		}
	}
		
	protected abstract void processHeader(Packet packet) throws OggException;
	protected abstract void processPacket(Packet packet) throws OggException;
	
	//Getters	
	@Override
	public double getEndTime() {
		return (stream != null ? stream.getEndTime() : -1);
	}
	
	@Override
	public OggStream getStream() {
		return stream;
	}
	
	@Override
	public OggCodec getCodec() {
		return codec;
	}
	
	@Override
	public boolean hasReadHeaders() {
		return hasReadHeaders;
	}
	
	//Setters
	
}
