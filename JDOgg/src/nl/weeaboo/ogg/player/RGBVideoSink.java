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
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;

import nl.weeaboo.ogg.theora.VideoFrame;

public class RGBVideoSink implements VideoSink {

	private final AtomicReference<IntBuffer> pixels;
	private final Dimension size;
	
	public RGBVideoSink() {
		pixels = new AtomicReference<IntBuffer>();
		size = new Dimension();
	}
	
	//Functions	
	@Override
	public void display(VideoFrame videoFrame) {
		size.setSize(videoFrame.getWidth(), videoFrame.getHeight());
		pixels.set(videoFrame.getRGB());
	}			
	
	//Getters
	public IntBuffer get() {		
		return pixels.getAndSet(null);
	}
	public int getImageWidth() {
		return size.width;
	}
	public int getImageHeight() {
		return size.height;
	}
	
	//Setters

}
