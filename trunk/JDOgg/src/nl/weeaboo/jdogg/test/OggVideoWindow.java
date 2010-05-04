package nl.weeaboo.jdogg.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class OggVideoWindow extends JFrame {

	private OggVideoPanel videoPanel;
	private JTextArea subsLabel;
	private JSlider slider;
	private JLabel timeLabel;

	private volatile double requestedPos = -1;
	
	public OggVideoWindow(String title) {
		setTitle(title);		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		videoPanel = new OggVideoPanel();
		
		add(videoPanel, BorderLayout.CENTER);
		add(createBottomPanel(), BorderLayout.SOUTH);
		
		setSize(800, 600);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	//Functions
	private JPanel createBottomPanel() {
		subsLabel = new JTextArea(2, 1);
		subsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
		subsLabel.setCaretColor(new Color(0, 0, 0, 0));
		subsLabel.setEditable(false);
		subsLabel.setBackground(Color.BLACK);
		subsLabel.setForeground(Color.WHITE);
		subsLabel.setFont(new Font("tahoma", Font.BOLD, 16));
		
		slider = new JSlider();		
		slider.setBorder(new EmptyBorder(5, 5, 5, 5));
		slider.setMinimum(0);
		slider.setMaximum(1000000);
		slider.setEnabled(false);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (slider.isEnabled() && slider.getValueIsAdjusting()) {
					requestedPos = slider.getValue() / (double)slider.getMaximum();
				}
			}
		});
		
		timeLabel = new JLabel();
		timeLabel.setHorizontalTextPosition(JLabel.RIGHT);
		
		JPanel sliderPanel = new JPanel(new BorderLayout(10, 10));
		sliderPanel.setBorder(new EmptyBorder(0, 10, 5, 10));
		sliderPanel.add(slider, BorderLayout.CENTER);
		sliderPanel.add(timeLabel, BorderLayout.EAST);
		
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		//panel.add(subsLabel, BorderLayout.CENTER);
		panel.add(sliderPanel, BorderLayout.SOUTH);		
		return panel;
	}

	//Getters
	public double getRequestedPos() {
		if (slider.getValueIsAdjusting()) {
			return -1;
		}
		
		double r = requestedPos;
		requestedPos = -1;
		return r;
	}
	
	public OggVideoPanel getVideoPanel() {
		return videoPanel;
	}
		
	//Setters	
	public void setSubtitles(final String txt) {
		synchronized (getTreeLock()) {
			videoPanel.setSubtitles(txt);
			if (!subsLabel.getText().equals(txt)) {
				subsLabel.setText(txt);
			}
		}
	}
	
	public void setPosition(final double pos, final double maxPos) {
		if (requestedPos >= 0) {
			return;
		}
		
		synchronized (getTreeLock()) {
			int p = 0;
			if (maxPos > 0) {
				p = (int)Math.round(1000000.0 * pos / maxPos);

				slider.setEnabled(false);
				slider.setMaximum(1000000);
				slider.setValue(p);
				slider.setEnabled(true);					
			} else {
				slider.setEnabled(false);
			}					

			StringBuilder sb = new StringBuilder();
			sb.append(time(pos));
			if (maxPos > 0) {
				sb.append("/" + time(maxPos));
			}				
			timeLabel.setText(sb.toString());
		}
	}
	
	private String time(double time) {
		int seconds = (int)Math.round(time);
		int minutes = seconds / 60;
		int hours = minutes / 60;
		
		seconds %= 60;
		minutes %= 60;

		StringBuilder sb = new StringBuilder();
		if (hours > 0) {
			sb.append(String.format("%02d:", hours));
		}
		sb.append(String.format("%02d:", minutes));
		sb.append(String.format("%02d", seconds));
		
		return sb.toString();
	}
	
}
