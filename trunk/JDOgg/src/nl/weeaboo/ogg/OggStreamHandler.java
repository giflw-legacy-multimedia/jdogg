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

import com.jcraft.jogg.Packet;

public interface OggStreamHandler<T> {
	
	public void process(Packet packet) throws OggException;

	public void reset();
	public void clearBuffer();
	public T read() throws IOException;
	public boolean available();
	public boolean trySync() throws OggException;
	public boolean trySkipTo(double time) throws OggException;
	
	public OggCodec getCodec();
	public boolean hasReadHeaders();
	public boolean isUnsynced();
	
	public double getTime();
	public double getEndTime();
	
	public void setStream(OggStream stream);
	
}
