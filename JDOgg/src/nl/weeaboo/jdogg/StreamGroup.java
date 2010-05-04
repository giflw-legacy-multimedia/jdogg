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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StreamGroup {

	private OggDecoder oggDecoder;
	private List<OggStream> streams;
	private StreamSelectorListener listener;
	private boolean sealed;
	
	public StreamGroup(OggDecoder oggd, StreamSelectorListener sgl) {
		oggDecoder = oggd;
		streams = new ArrayList<OggStream>();
		listener = sgl;
	}
	
	//Functions
	public void reset() {
		streams.clear();
		sealed = false;
	}
	
	public void add(OggStream s) {
		if (isSealed()) {
			throw new IllegalStateException("Can't add streams after seal");
		}
		
		streams.add(s);
	}
	
	public void seal() {
		if (!sealed) {
			sealed = true;
			
			listener.onSealed(this);
		}
	}
	
	//Getters
	public OggDecoder getOggDecoder() {
		return oggDecoder;
	}
	public Collection<OggStream> getStreams() {
		return Collections.unmodifiableList(streams);
	}
	public boolean isSealed() {
		return sealed;
	}
	
	//Setters
	
}
