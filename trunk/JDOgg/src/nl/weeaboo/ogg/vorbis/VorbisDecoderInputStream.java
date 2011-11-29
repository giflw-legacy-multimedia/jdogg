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

import nl.weeaboo.ogg.CircularBuffer;
import nl.weeaboo.ogg.CircularByteBuffer;
import nl.weeaboo.ogg.OggReader;

public class VorbisDecoderInputStream extends InputStream {
	
	protected final OggReader oggReader;
	protected final VorbisDecoder vorbisDecoder;
	private CircularBuffer buf;
	
	public VorbisDecoderInputStream(OggReader oggd, VorbisDecoder vorbisd) {
		oggReader = oggd;
		vorbisDecoder = vorbisd;
		
		buf = new CircularByteBuffer(8 << 10);
	}
	
	//Functions	
	@Override
	public int read() throws IOException {
		byte[] temp = new byte[1];
		int r = read(temp, 0, 1);
		if (r <= 0) return -1;
		return temp[0] & 0xFF;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		while (buf.size() <= 0) {
			while (!oggReader.isEOF() && !vorbisDecoder.available()) {
				oggReader.read();
			}			
			if (oggReader.isEOF()) {
				return -1;
			}		
			
			vorbisDecoder.read(buf);
		}
		
		return buf.get(b, off, len);
	}
	
	public void seekTo(double time) throws IOException {
		oggReader.seekExactTime(vorbisDecoder, time);
	}
	
	//Getters
	public double getTime() {
		return vorbisDecoder.getTime();
	}
	public double getEndTime() {
		return vorbisDecoder.getEndTime();
	}
	
	//Setters
	
}
