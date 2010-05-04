package nl.weeaboo.jdogg.kate;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.UnsupportedEncodingException;

import com.fluendo.jkate.Event;
import com.fluendo.jkate.KateSpaceMetric;
import com.fluendo.jkate.KateTextEncoding;
import com.fluendo.jkate.Region;

public class KateEvent {

	private Event event;
	
	public KateEvent(Event e) {
		event = e;
	}
	
	//Functions
	
	//Getters
	public String getText() throws UnsupportedEncodingException {
		KateTextEncoding enc = event.text_encoding;
		if (!KateTextEncoding.kate_utf8.equals(enc)) {
			throw new UnsupportedEncodingException("Kate subtitle encoding must be UTF-8");
		}
		
		return new String(event.text, 0, event.text.length, "UTF-8");
	}
	
	public double getStartTime() {
		return event.start_time;
	}
	public double getEndTime() {
		return event.end_time;
	}
	
	public boolean getClip() {
		return event.kr.clip;
	}
	
	public Rectangle getRegion(Dimension screen) {
		Rectangle r = new Rectangle(0, 0, screen.width, screen.height);

		Region region = event.kr;		
		if (region.metric == KateSpaceMetric.kate_metric_percentage) {
			r.x      = (int)Math.round(region.x * screen.width  / 100.0);
			r.y      = (int)Math.round(region.y * screen.height / 100.0);
			r.width  = (int)Math.round(region.w * screen.width  / 100.0);
			r.height = (int)Math.round(region.h * screen.height / 100.0);
		} else if (region.metric == KateSpaceMetric.kate_metric_millionths) {
			r.x      = (int)Math.round(region.x * screen.width  / 1000000.0);
			r.y      = (int)Math.round(region.y * screen.height / 1000000.0);
			r.width  = (int)Math.round(region.w * screen.width  / 1000000.0);
			r.height = (int)Math.round(region.h * screen.height / 1000000.0);
		} else if (region.metric == KateSpaceMetric.kate_metric_pixels) {
			r.setBounds(region.x, region.y, region.w, region.h);
		} else {
			//Invalid Metric!			
		}

		return r;
	}
	
	//Setters
	
}
