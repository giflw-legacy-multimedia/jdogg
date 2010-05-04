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

package nl.weeaboo.jdogg;

import com.jcraft.jogg.Packet;

public class StreamUtil {

	public static Packet clone(Packet p) {
		Packet r = new Packet();

		r.b_o_s = p.b_o_s;
		r.e_o_s = p.e_o_s;
		r.granulepos = p.granulepos;
		r.packetno = p.packetno;
		r.packet = p.packet;
		r.packet_base = p.packet_base.clone();
		r.bytes = p.bytes;
		
		return r;
	}
	
}
