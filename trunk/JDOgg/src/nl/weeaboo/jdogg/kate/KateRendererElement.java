package nl.weeaboo.jdogg.kate;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.UnsupportedEncodingException;

public class KateRendererElement {

	private KateEvent event;
	
	public KateRendererElement(KateEvent e) {
		event = e;
	}
	
	//Functions
	public void update(double time) {
		
	}
	
	//Getters
	public String getText() {
		try {
			return event.getText();
		} catch (UnsupportedEncodingException e) {
			return "EncodingException";
		}
	}
	
	public Rectangle getBounds(Dimension screen) {
		return event.getRegion(screen);
	}
	
	public double getStartTime() {
		return event.getStartTime();
	}
	
	public double getEndTime() {
		return event.getEndTime();
	}
	
	//Setters
	
}
