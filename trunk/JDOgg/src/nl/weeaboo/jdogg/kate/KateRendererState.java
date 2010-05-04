package nl.weeaboo.jdogg.kate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class KateRendererState {

	private List<KateRendererElement> elements;
	private double time;
	
	public KateRendererState() {
		elements = new LinkedList<KateRendererElement>();
	}
	
	//Functions
	public void addElement(KateRendererElement e) {
		elements.add(e);
	}
	public void removeElement(KateRendererElement e) {
		elements.remove(e);
	}
	
	public void reset() {
		elements.clear();
	}
	
	public void update(double t) {
		time = t;
		
		Iterator<KateRendererElement> it = elements.iterator();
		while (it.hasNext()) {
			KateRendererElement elem = it.next();
			
			if (time < elem.getEndTime()) {
				if (time >= elem.getStartTime()) {
					elem.update(time);
				}
			} else {
				it.remove();
			}
		}
	}
	
	//Getters
	public String getText(int lines) {
		String blob = getText();
		
		int l = 0;
		int index = blob.length();
		while ((index = blob.lastIndexOf('\n', index-1)) >= 0) {			
			l++;
			
			if (l >= lines) {
				return blob.substring(index+1);
			}
		}
		
		return blob;
	}
	public String getText() {	
		StringBuilder sb = new StringBuilder();
		for (KateRendererElement re : elements) {
			String text = re.getText();
			if (!text.trim().equals("")) {
				if (sb.length() > 0) {
					sb.append('\n');
				}
				sb.append(text);
			}
		}
		return sb.toString();
	}
	public double getTime() {
		return time;
	}
	
	public List<KateRendererElement> getElements() {
		return elements;
	}
	
	//Setters
	
}
