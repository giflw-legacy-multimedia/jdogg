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

package nl.weeaboo.ogg.player;

import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultVideoSink implements VideoSink {

	private AtomicReference<int[]> pixels;
	private Dimension size;
	
	public DefaultVideoSink() {
		pixels = new AtomicReference<int[]>();
		size = new Dimension();
	}
	
	//Functions	
	public void imageComplete(int status) {
	}
	
	//Getters
	public int[] get() {		
		return pixels.getAndSet(null);
	}
	public int getImageWidth() {
		return size.width;
	}
	public int getImageHeight() {
		return size.height;
	}
	
	//Setters
	public void setDimensions(int width, int height) {
		size = new Dimension(width, height);
	}
	
	public void setPixels(int x, int y, int w, int h, ColorModel model, int[] argb, int off, int scansize) {
		pixels.set(argb);
	}
	
	public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
		IntBuffer intbuf = ByteBuffer.wrap(pixels).asIntBuffer();
		int[] argb = new int[intbuf.remaining()];
		for (int n = 0; n < argb.length; n++) {
			argb[n] = intbuf.get();
		}
		setPixels(x, y, w, h, model, argb, off, scansize);
	}
	
	public void setColorModel(ColorModel model) {
	}
	
	public void setHints(int hintflags) {
	}
	
	public void setProperties(Hashtable<?, ?> props) {				
	}			
		

}
