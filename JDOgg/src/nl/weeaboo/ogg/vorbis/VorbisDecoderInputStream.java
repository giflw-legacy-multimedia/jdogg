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

package nl.weeaboo.ogg.vorbis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import nl.weeaboo.ogg.OggReader;

public class VorbisDecoderInputStream extends InputStream {

	protected final OggReader oggReader;
	protected final VorbisDecoder vorbisDecoder;
	private ByteBuffer buf;
	
	public VorbisDecoderInputStream(OggReader oggd, VorbisDecoder vorbisd) {
		oggReader = oggd;
		vorbisDecoder = vorbisd;
		
		buf = ByteBuffer.allocate(0);
	}
	
	//Functions
	@Override
	public int read() throws IOException {
		while (buf.remaining() <= 0) {
			while (!oggReader.isEOF() && !vorbisDecoder.available()) {
				oggReader.read();
			}			
			if (oggReader.isEOF()) {
				return -1;
			}		
			
			buf = ByteBuffer.wrap(vorbisDecoder.read());
		}
		return (buf.get() & 0xFF);
	}
	
	//Getters
	
	//Setters
	
}
