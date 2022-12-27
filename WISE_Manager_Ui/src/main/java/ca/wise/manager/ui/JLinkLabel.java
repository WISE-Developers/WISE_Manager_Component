package ca.wise.manager.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class JLinkLabel extends JLabel implements MouseListener {
	private static final long serialVersionUID = 1L;
	
	private List<LinkClicked> listeners = new ArrayList<>();

	public JLinkLabel(String text) {
		super(text);
		setForeground(new Color(0, 0, 153));
		setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		addMouseListener(this);
	}
	
	public JLinkLabel() {
		this("");
	}
	
	/**
	 * Add a listener for link clicked events.
	 */
	public void addLinkClickedListener(LinkClicked listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remote a listener for link clicked events.
	 */
	public void removeLinkClickedListener(LinkClicked listener) {
		listeners.remove(listener);
	}
	
	@FunctionalInterface
	interface LinkClicked {
		
		void OnLinkClicked(String link);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		listeners.forEach(l -> l.OnLinkClicked(getText()));
	}

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }

	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }
}
