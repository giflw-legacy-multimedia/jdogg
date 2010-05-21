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
import java.io.InputStream;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.SyncState;

public class PageReader {

	private InputStream inputStream;
	private int readBufSize;
	private SyncState syncState;
	private boolean eof;

	private long seek;
	private long seekLimit;
	private boolean unsynced;
	
	public PageReader() {
		readBufSize = 8192;
		syncState = new SyncState();
	}
	
	//Functions
	public void reset() {
		syncState.reset();
		seek = 0;
		unsynced = true;
		eof = false;
	}
	
	protected void readFromInput(boolean nonBlocking) throws IOException {
		int off = syncState.buffer(readBufSize);
		int maxRead = readBufSize;
		if (nonBlocking) {
			maxRead = Math.min(inputStream.available(), maxRead);
		}
		
		//Read some data
		int r = inputStream.read(syncState.data, off, maxRead);		
		if (r < 0) {
			eof = true;
			return;
		}
		
		//Pass the newly read data to the SyncState
		syncState.wrote(r);		
	}
	
	public boolean read(Page page, boolean nonBlocking) throws IOException {
		while (!isEOF()) {
			int res = syncState.pageseek(page);				
			if (res < 0) {
				//Unsynced, skipped (-retval) bytes
				seek += (-res);
				unsynced = true;
				if (seek >= seekLimit) {
					throw new OggException("Unable to find a page within the seekLimit");
				}
			} else if (res == 0) {
				//Needs more data
				readFromInput(nonBlocking);
			} else {
				//Page read
				seek = 0;
				unsynced = false;
				return true;
			}
		}
		return false;
	}
	
	//Getters
	public boolean isUnsynced() {
		return unsynced;
	}
	public boolean isEOF() {
		return eof;
	}
	
	//Setters
	public void setInput(InputStream in) {
		setInput(in, Integer.MAX_VALUE);
	}
	
	/**
	 * @param seekLim The maximum number of bytes that may be skipped before a
	 *        new sync point is found.
	 */
	public void setInput(InputStream in, int seekLim) {
		inputStream = in;
		seekLimit = seekLim;
		reset();
	}
	
}
