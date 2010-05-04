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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class MultiInput {

	private InputStream inputStream;
	private FileChannel channel;
	
	private long position;
	private long limit;
	private boolean eof;
	
	public MultiInput(File file) throws FileNotFoundException {	
		this(new FileInputStream(file), file.length());
	}

	public MultiInput(InputStream in, long length) {
		inputStream = in;
		limit = length;
		
		if (in instanceof FileInputStream) {
			FileInputStream fin = (FileInputStream)in;
			channel = fin.getChannel();
			try {
				if (length < 0) length = channel.size();
			} catch (IOException ioe) {
				//e.printStackTrace();
			}
		}
	}
		
	//Functions
	
	//Getters
	public int read(byte out[], int off, int len) throws IOException {
		int r = inputStream.read(out, off, len);
		if (r >= 0) {
			position += r;
		} else {
			eof = true;
		}
		return r;
	}
	
	public boolean isSeekable() {
		return channel != null;
	}
	
	public long getPosition() {
		return position;
	}
	
	public long getLimit() {
		return limit;
	}
	
	public boolean isEOF() {
		return eof;		
	}
	
	//Setters
	public void setPosition(long p) throws IOException {
		if (!isSeekable()) {
			throw new IOException("MultiInput is not seekable");
		}
		
		position = p;
		channel.position(position);
		eof = (getPosition() >= getLimit());
	}
	
}
