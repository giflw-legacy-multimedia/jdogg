package nl.weeaboo.jdogg.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.MemoryImageSource;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Hashtable;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class VideoPanel extends JPanel implements ImageConsumer {

	private Image image;
	private MemoryImageSource imageModel;
	private Dimension size;
	
	private Font subfont;
	private String subtitles;
	
	public VideoPanel() {
		setBackground(Color.BLACK);
		
		subfont = new Font("tahoma", Font.BOLD, 12);
		subtitles = "";
	}
	
	//Functions
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		if (size == null || image == null) {
			return;
		}
		
		Graphics2D g = (Graphics2D)graphics;
		
		int width = getWidth();
		int height = getHeight();
		
		float scalex = width  / (float)size.width;
		float scaley = height / (float)size.height;
		float scale = Math.min(scalex, scaley);
		
		int w = Math.round(scale * size.width);
		int h = Math.round(scale * size.height);
		int x = (width - w) / 2;
		int y = (height - h) / 2;
		
		g.drawImage(image, x, y, w, h, this);
		
		paintSubtitles(g, x, y, w, h);
	}
	
	protected void paintSubtitles(Graphics2D g, int x, int y, int w, int h) {
		if (subtitles == null || subtitles.trim().length() == 0) {
			return;
		}
		
		float sz = Math.min(w, h) / 24f;

		Rectangle r = null;
		
		while (r == null) {
			g.setFont(subfont.deriveFont(sz));
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			r = g.getFontMetrics().getStringBounds(subtitles, g).getBounds();
			
			if (r.width >= w || r.height >= h) {
				//TODO: Solve fitting problems with automatic line breaks
			}
		}

		int sx = x + (w - r.width) / 2;
		int sy = y + Math.round(h * 0.95f);
		
		g.setColor(new Color(0, 0, 0, 0.75f));
		//g.drawString(subtitles, Math.round(sx + sz/16f), Math.round(sy + sz/16f));
		g.fillRect(sx - 10, Math.round(sy - sz),
				r.width + 20, Math.round(sz + 10));
		
		g.setColor(new Color(255, 224, 64));
		g.drawString(subtitles, sx, sy);
	}
	
	public void imageComplete(int status) {
		repaint();
	}
	
	public void setDimensions(int width, int height) {
		size = new Dimension(width, height);
		
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		float scalex = (ss.width  >> 1) / (float)size.width;
		float scaley = (ss.height >> 1) / (float)size.height;
		float scale = Math.min(scalex, scaley);
		
		setPreferredSize(new Dimension(Math.round(width * scale), Math.round(height * scale)));		
	}
	
	public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
		if (imageModel == null) {
			imageModel = new MemoryImageSource(w, h, pixels, off, scansize);
			imageModel.setAnimated(true);
			image = Toolkit.getDefaultToolkit().createImage(imageModel);
		} else {
			imageModel.newPixels(pixels, model, off, scansize);
		}
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
	
	public void setSubtitles(String text) {
		subtitles = text;
	}
	
}
