/* JDOgg
 * 
 * Copyright (c) 2011 Timon Bijlsma
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

import java.nio.ByteBuffer;

public class CircularByteBuffer implements CircularBuffer {

	private ByteBuffer buffer;
	private int capacity;
	private int maxCapacity;
	private int begin, end;
	
	public CircularByteBuffer(int initialCapacity, int initialMaxCapacity) {
		capacity = initialCapacity;
		maxCapacity = initialMaxCapacity;
	}
	
	//Functions
	protected ByteBuffer allocateBuffer(int capacity) {
		return ByteBuffer.allocate(capacity);
	}

	private void ensureBufferCapacity(int minCapacity) {
		if (buffer == null || capacity <= minCapacity) {
			int maxCapacity = getMaxCapacity();
			int desiredCapacity = Math.max(Math.min(maxCapacity, capacity * (buffer==null?1:2)), minCapacity+1);
			if (desiredCapacity > maxCapacity) {
				throw new IllegalStateException("Required capacity (" + minCapacity + ") >= maxCapacity (" + maxCapacity + ")");
			}
						
			ByteBuffer b = allocateBuffer(desiredCapacity);
			int r = get(b);
			buffer = b;
			begin = 0;
			end = r;
			capacity = desiredCapacity;
		}
	}
	
	public void clear() {
		begin = end = 0;
	}
	
	protected int wrap(int val) {
		return val % capacity;
	}
	
	@Override
	public void put(ByteBuffer in) {
		if (in.hasArray()) {
			put(in.array(), in.arrayOffset()+in.position(), in.remaining());
			return;
		}
		
		int len = in.remaining();
		ensureBufferCapacity(size() + len);
		
		buffer.position(end);
		if (end >= begin) {
			int a = Math.min(len, capacity-end);
			for (int n = 0; n < a; n++) {
				buffer.put(in.get());
			}
			buffer.position(0);
			for (int n = 0; n < len-a; n++) {
				buffer.put(in.get());
			}
		} else {
			for (int n = 0; n < len; n++) {
				buffer.put(in.get());
			}
		}
		
		end = wrap(end + len);
	}
	
	@Override
	public void put(byte[] b, int off, int len) {
		ensureBufferCapacity(size() + len);
		
		buffer.position(end);
		if (end >= begin) {
			int a = Math.min(len, capacity-end);
			buffer.put(b, off, a);
			buffer.position(0);
			buffer.put(b, off+a, len-a);			
		} else {
			buffer.put(b, off, len);
		}

		end = wrap(end + len);
	}

	@Override
	public int get(ByteBuffer out) {
		if (out.hasArray()) {
			return get(out.array(), out.arrayOffset()+out.position(), out.remaining());
		}		
		if (buffer == null) {
			return 0;
		}
		
		int len = Math.min(out.remaining(), size());
		
		buffer.position(begin);
		if (end >= begin) {
			for (int n = 0; n < len; n++) {
				out.put(buffer.get());
			}
		} else {
			int a = Math.min(len, capacity-begin);
			for (int n = 0; n < len; n++) {
				out.put(buffer.get());
			}
			if (a < len) {
				buffer.position(0);
				for (int n = 0; n < len-a; n++) {
					out.put(buffer.get());
				}
			}
		}

		begin = wrap(begin + len);		
		return len;
	}
	
	@Override
	public int get(byte[] out, int off, int len) {
		if (buffer == null) return 0;
		
		len = Math.min(len, size());		
		buffer.position(begin);
		if (end >= begin) {
			buffer.get(out, off, len);
		} else {
			int a = Math.min(len, capacity-begin);
			buffer.get(out, off, a);
			if (a < len) {
				buffer.position(0);
				buffer.get(out, off+a, len-a);
			}
		}
				
		begin = wrap(begin + len);
		return len;
	}
	
	@Override
	public int skip(int bytes) {
		if (bytes <= 0) return 0;
		
		bytes = Math.min(size(), bytes);		
		begin = wrap(begin + bytes);
		return bytes;
	}

	//Getters
	@Override
	public int size() {
		if (end >= begin) {
			return end-begin;
		} else {
			return end + capacity - begin;
		}
	}

	@Override
	public int getCapacity() {
		return capacity;
	}
	
	public int getMaxCapacity() {
		return maxCapacity;
	}
	
	//Setters
	public void setMaxCapacity(int max) {
		if (maxCapacity > max) {
			throw new IllegalArgumentException("Can't decrease the maximum capacity");
		}
		
		maxCapacity = max;
	}
	
}
