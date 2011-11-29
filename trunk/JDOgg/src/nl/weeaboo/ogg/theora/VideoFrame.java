package nl.weeaboo.ogg.theora;

import java.nio.IntBuffer;

import nl.weeaboo.ogg.player.YUVBufferPixelGrabber;

import com.fluendo.jheora.YUVBuffer;

public class VideoFrame {

	private YUVBuffer yuvBuffer;
	private IntBuffer rgb;
	
	private final int width;
	private final int height;
	private final double startTime;
	private final double duration;
	
	public VideoFrame(YUVBuffer buf, int w, int h, double start, double dur) {
		width = w;
		height = h;
		yuvBuffer = buf;
		startTime = start;
		duration = dur;
		
		/*
		synchronized (buf) {
			yuvBuffer = new YUVBuffer();
			yuvBuffer.data = buf.data.clone();
			yuvBuffer.u_offset = buf.u_offset;
			yuvBuffer.uv_height = buf.uv_height;
			yuvBuffer.uv_stride = buf.uv_stride;
			yuvBuffer.uv_width = buf.uv_width;
			yuvBuffer.v_offset = buf.v_offset;
			yuvBuffer.y_height = buf.y_height;
			yuvBuffer.y_offset = buf.y_offset;
			yuvBuffer.y_stride = buf.y_stride;
			yuvBuffer.y_width = buf.y_width;
		}
		*/				
	}
	
	//Functions	
	
	//Getters
	public YUVBuffer getYUV() {
		return yuvBuffer;
	}
	public IntBuffer getRGB() {
		if (rgb == null) {
			synchronized (yuvBuffer) {
				rgb = YUVBufferPixelGrabber.getPixelsRGB(yuvBuffer);
			}
		}
		return rgb;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public double getStartTime() {
		return startTime;
	}
	public double getEndTime() {
		return startTime + getDuration();
	}
	public double getDuration() {
		return duration;
	}
	
	//Setters
	
}
