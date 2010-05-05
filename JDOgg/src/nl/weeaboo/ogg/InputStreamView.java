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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamView extends FilterInputStream {

	private long markPosition;
	private long position;
	private long limit;
	private boolean closed;
	
	public InputStreamView(InputStream in, long len) {
		super(in);
		
		limit = len;
	}

	//Functions
	protected void checkClosed() throws IOException {
		if (closed) {
			throw new IOException("InputStreamView is closed");
		}
	}
	
	@Override
	public void close() {
		closed = true;
	}
	
	@Override
	public void reset() throws IOException {
		checkClosed();
		
		super.reset();
		
		position = markPosition;
	}
	
	@Override
	public void mark(int readLimit) {
		if (!closed && markSupported()) {
			super.mark(Math.min(remainingAsInt(), readLimit));
			markPosition = position;
		}
	}
	
	@Override
	public long skip(long s) throws IOException {
		checkClosed();

		if (remaining() <= 0) {
			return -1;
		}
		
		s = skip(Math.min(remaining(), s));
		if (s > 0) {
			position += s;
		}
		
		return s;
	}
	
	@Override
	public int read() throws IOException {
		checkClosed();

		if (remaining() <= 0) {
			return -1;
		}
		position++;
		
		return super.read();
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		checkClosed();

		if (remaining() <= 0) {
			return -1;
		}
		
		int r = super.read(b, off, Math.min(remainingAsInt(), len));
		if (r > 0) {
			position += r;
		}
		return r;
	}
	
	//Getters
	protected long remaining() {
		return (limit < 0 ? Long.MAX_VALUE : limit - position);
	}
	protected int remainingAsInt() {
		return (int)Math.min(Integer.MAX_VALUE, remaining());		
	}
	
	//Setters
	
}
