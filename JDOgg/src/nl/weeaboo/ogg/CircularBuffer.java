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

public interface CircularBuffer {

	//Functions
	public void clear();
	public void put(ByteBuffer in);
	public void put(byte[] b, int off, int len);
	public int skip(int bytes);
	public int get(ByteBuffer out);
	public int get(byte[] out, int off, int len);
	
	//Getters
	public int size();
	public int getCapacity();
	
	//Setters
	
}
