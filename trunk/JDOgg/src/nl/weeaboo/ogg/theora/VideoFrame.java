package nl.weeaboo.ogg.theora;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Hashtable;

import com.fluendo.jheora.YUVBuffer;

public class VideoFrame {

	YUVBuffer yuvBuffer;
	
	private int width;
	private int height;
	private double startTime;
	private double duration;
	
	private IntBuffer argb;
	
	public VideoFrame(YUVBuffer buf, int w, int h, double startTime, double duration) {
		yuvBuffer = buf;
		yuvBuffer.data = yuvBuffer.data.clone();
		
		width = w;
		height = h;
		
		this.startTime = startTime;
		this.duration = duration;
	}
	
	//Functions	
	public IntBuffer readPixels() {
		return readPixels(null);
	}
	public IntBuffer readPixels(final ImageConsumer ic) {
		ImageConsumer consumer = new ImageConsumer() {
			public void setColorModel(ColorModel model) {
				if (ic != null) ic.setColorModel(model);
			}
			public void setDimensions(int width, int height) {
				if (ic != null) ic.setDimensions(width, height);
			}
			public void setHints(int hintflags) {
				if (ic != null) ic.setHints(hintflags);
			}
			public void setProperties(Hashtable<?, ?> props) {
				if (ic != null) ic.setProperties(props);
			}			
			public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
				if (ic != null) ic.setPixels(x, y, w, h, model, pixels, off, scansize);
				argb = ByteBuffer.wrap(pixels).asIntBuffer();
			}			
			public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
				if (ic != null) ic.setPixels(x, y, w, h, model, pixels, off, scansize);
				argb = IntBuffer.wrap(pixels);
			}
			public void imageComplete(int status) {
				if (ic != null) ic.imageComplete(status);
			}
		};
		
		yuvBuffer.startProduction(consumer);
		
		return argb;
	}
	
	//Getters
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
