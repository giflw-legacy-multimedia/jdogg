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

import java.util.ArrayDeque;
import java.util.Queue;

import com.jcraft.jogg.Packet;

public abstract class AbstractOggStreamHandler<T> implements OggStreamHandler<T> {

	private OggCodec codec;
	protected OggStream stream;
	private final boolean deferredHandling;		
	private boolean headersRead;
	
	protected Queue<Packet> packets;
	
	public AbstractOggStreamHandler(OggCodec c, boolean defer) {
		codec = c;
		deferredHandling = defer;
		packets = new ArrayDeque<Packet>();
	}
	
	//Functions
	protected abstract void onHeadersRead();
	
	@Override
	public void process(Packet packet) throws OggException {
		if (!hasReadHeaders()) {
			processHeader(packet);
			
			if (packet.packetno >= 2) {
				headersRead = true;
				onHeadersRead();
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
	
	protected abstract void processHeader(Packet packet) throws OggException;
	protected abstract void processPacket(Packet packet) throws OggException;
	
	//Getters
	@Override
	public OggCodec getCodec() {
		return codec;
	}
	
	@Override
	public boolean hasReadHeaders() {
		return headersRead;
	}
	
	//Setters
	@Override
	public void setStream(OggStream s) {
		stream = s;
	}
	
}
