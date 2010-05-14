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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileOggInput implements RandomOggInput {

	private FileInputStream fin;
	private FileChannel channel;
	
	private InputStreamView current;
	
	public FileOggInput(File file) throws FileNotFoundException {
		this(new FileInputStream(file));
	}
	public FileOggInput(FileInputStream fin) throws FileNotFoundException {	
		this.fin = fin;
		this.channel = fin.getChannel();
	}
	
	//Functions
	protected void closeCurrent() {
		if (current != null) {
			current.close();
			current = null;
		}		
	}
	
	@Override
	public void close() throws IOException {
		closeCurrent();
		
		fin.close();
	}
	
	@Override
	public InputStream openStream() throws IOException {
		return openStream(0, length());
	}

	@Override
	public InputStream openStream(long off, long len) throws IOException {
		closeCurrent();
		
		channel.position(off);
		current = new InputStreamView(fin, len);
		return current;
	}

	@Override
	public int read(byte[] b, int off, long foff, int len) throws IOException {
		closeCurrent();
		
		ByteBuffer buf = ByteBuffer.wrap(b, off, len);
		channel.position(foff);
		return channel.read(buf);
	}
		
	//Getters
	@Override
	public boolean isReadSlow() {
		return false;
	}
	
	@Override
	public boolean isSeekSlow() {
		return false;
	}
	
	@Override
	public long length() throws IOException {
		return channel.size();
	}
	
	//Setters
	
}
