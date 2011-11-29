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

package nl.weeaboo.ogg.player;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.nio.IntBuffer;
import java.util.Hashtable;

import com.fluendo.jheora.YUVBuffer;

public final class YUVBufferPixelGrabber {

	private YUVBufferPixelGrabber() {		
	}
	
	public static IntBuffer getPixelsRGB(YUVBuffer buf) {
		final IntBuffer[] result = new IntBuffer[1];
		
		buf.startProduction(new ImageConsumer() {
			public void setDimensions(int width, int height) {
			}
			public void setProperties(Hashtable<?, ?> props) {
			}
			public void setColorModel(ColorModel model) {
			}
			public void setHints(int hintflags) {
			}
			public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
			}
			public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
				result[0] = IntBuffer.wrap(pixels, off, scansize * (h-1) + w);
			}
			public void imageComplete(int status) {
			}
		});		
		
		return result[0];
	}
	
}
