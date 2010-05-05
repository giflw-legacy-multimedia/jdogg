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

package nl.weeaboo.ogg;

import javax.sound.sampled.AudioFormat;

public enum OggCodec {
	
	Vorbis(new byte[] {'v','o','r','b','i','s'}),
	Theora(new byte[] {'t','h','e','o','r','a'}),
	Kate(  new byte[] {'k','a','t','e'}),
	Unknown(new byte[]{'u','n','k','n','o','w','n'});
	
	public final byte signature[];
	public final AudioFormat.Encoding encoding;
	
	private OggCodec(byte sig[]) {
		this.signature = sig;
		this.encoding = new AudioFormat.Encoding(toString());
	}
	
	public static OggCodec fromSignature(byte sig[], int off, int len)  {
		off += 1;
		len -= 1;
		
		for (OggCodec codec : values()) {
			boolean ok = true;
			
			for (int n = 0; n < Math.min(codec.signature.length, len); n++) {
				if (codec.signature[n] != sig[off + n]) {
					ok = false;
					break;
				}
			}
			
			if (ok) {
				return codec;
			}
		}
		
		//System.out.println(new String(sig, off, len));
		return Unknown;
	}
}
