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

import com.fluendo.jheora.YUVBuffer;

public class YUVVideoSink implements VideoSink {

	private final AtomicReference<YUVBuffer> pixels;
	private final YUVBuffer buf;
	private final Dimension size;
	
	public YUVVideoSink() {
		pixels = new AtomicReference<YUVBuffer>();
		buf = new YUVBuffer();
		size = new Dimension();
	}
	
	//Functions	
	@Override
	public void display(VideoFrame videoFrame) {
		size.setSize(videoFrame.getWidth(), videoFrame.getHeight());
		
		YUVBuffer yuv = videoFrame.getYUV();
		if (yuv != null) {
			synchronized (buf) {
				synchronized (yuv) {
					if (buf.data == null || buf.data.length < yuv.data.length) {
						buf.data = new short[yuv.data.length];
					}
					System.arraycopy(yuv.data, 0, buf.data, 0, yuv.data.length);
					
					buf.y_offset  = yuv.y_offset;
					buf.y_width   = yuv.y_width;
					buf.y_height  = yuv.y_height;
					buf.y_stride  = yuv.y_stride;
					buf.u_offset  = yuv.u_offset;
					buf.v_offset  = yuv.v_offset;
					buf.uv_width  = yuv.uv_width;
					buf.uv_height = yuv.uv_height;
					buf.uv_stride = yuv.uv_stride;
				}
					
				buf.newPixels();
			}
			
			pixels.set(buf);
		}
	}
	
	public IntBuffer convertToRGB(YUVBuffer buf) {
		return YUVBufferPixelGrabber.getPixelsRGB(buf);
	}
	
	//Getters
	public YUVBuffer get() {		
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
