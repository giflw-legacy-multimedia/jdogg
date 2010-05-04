/* JDOgg
 * 
 * Copyright (C) 2010 Timon Bijlsma
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.SyncState;

public class OggDecoder {

	//Settings
	private int readBufSize = 8192;
	
	//State
	private MultiInput input;
	private SyncState syncState;
	private Map<Integer, OggStream> streams;
	private OggStream primaryStream;
	private StreamGroup streamGroup;
	
	public OggDecoder() {
		resetState();
	}
	
	private void resetState() {
		syncState = new SyncState();
		streams = new HashMap<Integer, OggStream>();
	}
	
	public void readStreamGroup(StreamSelectorListener sgl) throws IOException {
		streamGroup = new StreamGroup(this, sgl);
		while (!isEOF() && !streamGroup.isSealed()) {
			update();
		}
		if (isEOF() && !streamGroup.isSealed()) {
			streamGroup.seal();
		}
		streamGroup = null;
	}
	
	public void readHeaders() throws IOException {
		while (!isEOF()) {
			boolean ok = true;
			for (OggStream stream : streams.values()) {
				OggStreamHandler handler = stream.getHandler();
				if (handler != null && !handler.hasReadHeaders()) {
					ok = false;
				}
			}
			if (ok) break;
			
			update();
		}
	}
	
	public void update() throws IOException {
		if (input == null) {
			throw new IOException("Input not set");
		}
		
		Page page = new Page();
				
		//Read some data
		int off = syncState.buffer(readBufSize);
		int r = input.read(syncState.data, off, readBufSize);		
		if (r < 0) {
			return; //End of data
		}
		
		//Pass the newly read data to the SyncState
		syncState.wrote(r);

		//Process the data we just read
		while (true) {
			int res = syncState.pageseek(page);				
			if (res < 0) {
				break; //Unsynced, skipped (-retval) bytes
			} else if (res == 0) {
				break; //Needs more data
			}
			
			//Get the correct stream by Id
			int streamId = page.serialno();
			OggStream stream = streams.get(streamId);				
			if (stream == null) {
				stream = new OggStream(streamId);
				streams.put(streamId, stream);
			}

			if (page.bos() != 0) {
				if (streamGroup != null) {
					if (streamGroup.isSealed()) {
						streamGroup.reset();
					}					
					streamGroup.add(stream);
				}
			} else {
				if (streamGroup != null && !streamGroup.isSealed()) {
					streamGroup.seal();
				}
			}
			
			//Pass page to the stream
			stream.process(page);
		}
	}
	
	/**
	 * Discards all current buffers and position-dependent information
	 */
	public void flush() {
		syncState.reset();
		for (OggStream stream : streams.values()) {
			stream.flush();
		}		
	}
	
	public void seekFrac(double frac) throws IOException {
		long bytepos;
		if (Math.abs(frac) <= 0.000001) {
			bytepos = 0;
		} else if (Math.abs(1.0 - frac) <= 0.000001) {
			bytepos = input.getLimit();
		} else {
			bytepos = Math.round(frac * input.getLimit());
		}
		
		input.setPosition(bytepos);
		
		flush();
		
		//TODO: Seek primary OggStream to frac.		
	}
	
	//Getters
	public boolean isSeekable() {
		return input != null && input.isSeekable();
	}
	
	public boolean isEOF() {
		return input == null || input.isEOF();
	}
	
	public OggStream getPrimaryStream() {
		return primaryStream;
	}
	
	public double getTime() {
		if (primaryStream != null) {
			return primaryStream.getTime();
		}
		return -1;
	}
	
	public double getEndTime() {
		if (primaryStream != null) {
			return primaryStream.getEndTime();
		}
		return 0;
	}
	
	//Setters
	public void setInput(File file) throws FileNotFoundException {
		input = new MultiInput(file);
		
		resetState();
	}
	public void setInput(InputStream in, long length) {
		input = new MultiInput(in, length);
		
		resetState();
	}
	
	public void setPrimaryStream(OggStream s) {
		if (!streams.values().contains(s)) {
			throw new IllegalArgumentException("Stream argument is not a valid stream for this OggDecoder");
		}
		
		primaryStream = s;
	}
	
}
