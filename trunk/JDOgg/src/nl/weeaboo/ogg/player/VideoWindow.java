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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class VideoWindow extends JFrame {

	private CopyOnWriteArrayList<VideoWindowListener> videoWindowListeners;
	
	private VideoPanel videoPanel;
	private JTextArea subsLabel;
	private JToggleButton playPauseButton;
	private JSlider slider;
	private JLabel timeLabel;
	
	private ImageIcon playI, pauseI;
	
	public VideoWindow(String title) {
		videoWindowListeners = new CopyOnWriteArrayList<VideoWindowListener>();
				
		playI = new ImageIcon(getClass().getResource("res/play.png"));
		pauseI = new ImageIcon(getClass().getResource("res/pause.png"));
		
		videoPanel = new VideoPanel();		
		add(videoPanel, BorderLayout.CENTER);
		add(createBottomPanel(), BorderLayout.SOUTH);
		
		setTitle(title);		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	//Functions
	public void addVideoWindowListener(VideoWindowListener vwl) {
		synchronized (videoWindowListeners) {
			videoWindowListeners.add(vwl);
		}
	}
	public void removeVideoWindowListener(VideoWindowListener vwl) {
		synchronized (videoWindowListeners) {
			videoWindowListeners.remove(vwl);
		}
	}
	
	private JPanel createBottomPanel() {
		subsLabel = new JTextArea(2, 1);
		subsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
		subsLabel.setCaretColor(new Color(0, 0, 0, 0));
		subsLabel.setEditable(false);
		subsLabel.setBackground(Color.BLACK);
		subsLabel.setForeground(Color.WHITE);
		subsLabel.setFont(new Font("tahoma", Font.BOLD, 16));
		
		playPauseButton = new JToggleButton();
		playPauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playPauseButton.setIcon(playPauseButton.isSelected() ? pauseI : playI);

				boolean p = !playPauseButton.isSelected();
				synchronized (videoWindowListeners) {
					for (VideoWindowListener vwl : videoWindowListeners) {
						vwl.onPause(p);
					}
				}
			}
		});
		
		playPauseButton.setSelected(true);
		playPauseButton.setIcon(pauseI);
		
		slider = new JSlider();		
		slider.setBorder(new EmptyBorder(5, 5, 5, 5));
		slider.setMinimum(0);
		slider.setMaximum(1000000);
		slider.setEnabled(false);
		slider.getModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (slider.isEnabled() && !slider.getValueIsAdjusting()) {
					double frac = slider.getValue() / (double)slider.getMaximum();

					synchronized (videoWindowListeners) {
						for (VideoWindowListener vwl : videoWindowListeners) {
							vwl.onSeek(frac);
						}
					}		
				}
			}
		});
		
		timeLabel = new JLabel();
		timeLabel.setHorizontalTextPosition(JLabel.RIGHT);
		
		JPanel sliderPanel = new JPanel(new BorderLayout(10, 10));
		sliderPanel.setBorder(new EmptyBorder(0, 10, 5, 10));
		sliderPanel.add(playPauseButton, BorderLayout.WEST);
		sliderPanel.add(slider, BorderLayout.CENTER);
		sliderPanel.add(timeLabel, BorderLayout.EAST);
		
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		//panel.add(subsLabel, BorderLayout.CENTER);
		panel.add(sliderPanel, BorderLayout.SOUTH);		
		return panel;
	}

	//Getters	
	public VideoPanel getVideoPanel() {
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
	
	public void setPositionTime(final double pos, final double maxPos) {
		synchronized (getTreeLock()) {
			StringBuilder sb = new StringBuilder();
			sb.append(time(pos));
			if (maxPos > 0) {
				sb.append("/" + time(maxPos));
			}				
			timeLabel.setText(sb.toString());			
		}
	}
	
	public void setPositionBytes(final double pos, final double maxPos) {
		synchronized (getTreeLock()) {
			if (maxPos > 0) {
				slider.setEnabled(false);
				if (!slider.getValueIsAdjusting()) {
					int p = (int)Math.round(1000000.0 * pos / maxPos);
					slider.setMaximum(1000000);
					slider.setValue(p);
				}
			}
			slider.setEnabled(maxPos > 0);
		}
	}
	
	public void setPaused(boolean p) {
		synchronized (getTreeLock()) {
			playPauseButton.setSelected(!p);
			playPauseButton.setIcon(playPauseButton.isSelected() ? pauseI : playI);
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
