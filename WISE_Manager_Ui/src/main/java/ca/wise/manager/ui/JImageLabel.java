package ca.wise.manager.ui;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

public class JImageLabel extends JLabel implements MouseListener {
	private static final long serialVersionUID = 1L;
	
	private List<ButtonClicked> listeners = new ArrayList<>();
	private boolean selected = false;

	public JImageLabel(Icon icon) {
		super(icon);
		setForeground(new Color(0, 0, 153));
		addMouseListener(this);
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
	}
	
	/**
	 * Add a listener for link clicked events.
	 */
	public void addClickedListener(ButtonClicked listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remote a listener for link clicked events.
	 */
	public void removeClickedListener(ButtonClicked listener) {
		listeners.remove(listener);
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
		if (selected)
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(2, 2, 2, 2),
					BorderFactory.createLineBorder(Color.BLACK)));
		else
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (!enabled) {
			if (selected)
				setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(2, 2, 2, 2),
						BorderFactory.createLineBorder(Color.GRAY)));
			else
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		}
		else {
			if (selected)
				setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(2, 2, 2, 2),
						BorderFactory.createLineBorder(Color.BLACK)));
			else
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		}
	}
	
	@FunctionalInterface
	interface ButtonClicked {
		
		void OnLinkClicked(String link);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (isEnabled())
			listeners.forEach(l -> l.OnLinkClicked(getText()));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!selected && isEnabled())
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(2, 2, 2, 2),
					BorderFactory.createDashedBorder(Color.GRAY)));
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (!selected)
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
	}

	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }

}
