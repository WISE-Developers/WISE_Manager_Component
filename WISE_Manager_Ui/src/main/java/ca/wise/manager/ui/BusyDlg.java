package ca.wise.manager.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import ca.hss.platform.OperatingSystem;
import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;

public class BusyDlg extends JDialog {
	private static final long serialVersionUID = 1L;
	private BusyPanel panel;
	private int posX = 0;
	private int posY = 0;

	public BusyDlg(Window owner, final Runnable task) {
		super(owner);
		setResizable(false);
		setModal(true);
		initializeUi();
		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				new Thread(() -> {
					task.run();
					BusyDlg.this.setVisible(false);
				}).start();
			}
			@Override
			public void windowIconified(WindowEvent e) { }
			@Override
			public void windowDeiconified(WindowEvent e) { }
			@Override
			public void windowDeactivated(WindowEvent e) { }
			@Override
			public void windowActivated(WindowEvent e) { }
			@Override
			public void windowClosing(WindowEvent e) {}

			@Override
			public void windowClosed(WindowEvent e) {
				panel.stop();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				posX = e.getX();
				posY = e.getY();
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				BusyDlg.this.setLocation(e.getXOnScreen() - posX, e.getYOnScreen() - posY);
			}
		});
		setLocationRelativeTo(owner);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible)
			panel.start();
		super.setVisible(visible);
	}
	
	private void initializeUi() {
		setModalityType(ModalityType.DOCUMENT_MODAL);
		try {
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo.png")));
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo-40.png")));
			icons.add(ImageIO.read(getClass().getResource("/Prometheus-Logo-20.png")));
			setIconImages(icons);
		}
		catch (IOException e1) {
			WISELogger.getSpecial(LogName.Ui).debug("Unable to load icon", e1);
		}
		setTitle("Working");
		setBounds(0, 0, 116, 116);
		setBounds(0, 0, 126, 126);
		getRootPane().setOpaque(false);
		if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Linux) {
			getRootPane().setBorder(new LineBorder(new Color(173, 169, 165), 1, false));
		}
		else if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Mac) {
			getRootPane().setBorder(new LineBorder(new Color(144, 144, 144), 1, false));
		}
		else {
			getRootPane().setBorder(new CompoundBorder(new LineBorder(new Color(37, 44, 51), 1, true),
					new CompoundBorder(new LineBorder(new Color(246, 250, 254), 1, true),
					new CompoundBorder(new LineBorder(new Color(208, 229, 250), 1, true),
					new CompoundBorder(new LineBorder(new Color(207, 228, 250), 1, true),
					new CompoundBorder(new LineBorder(new Color(206, 227, 248), 1, false),
					new CompoundBorder(new LineBorder(new Color(204, 225, 247), 1, false),
					new CompoundBorder(new LineBorder(new Color(231, 241, 250), 1, false),
					new LineBorder(new Color(88, 102, 117), 1, false)))))))));
		}
		panel = new BusyPanel();
		add(panel);
	}

	private static class BusyPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private int counter = 0;
		private int counter2 = 0;
		private static final int numCircles = 502;
		private Timer timer;
		private Stroke stroke;
		private Color color = new Color(144, 41, 6);

		public BusyPanel() {
			super(true);
			setLayout(null);
			timer = new Timer();
			stroke = new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(100, 100);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(100, 100);
		}

		public void start() {
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					counter -= 10;
					counter2 += 6;
					repaint();
				}
			}, 25, 25);
		}

		public void stop() {
			timer.cancel();
		}
		
		private int length() {
			int size = counter2 % numCircles;
			if (size > (numCircles >> 1))
				size = numCircles - size;
			return (int)(-110.0f + (size * ((-25.0f + 110.0f) / (numCircles >> 1))));
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2.setColor(color);
			g2.setStroke(stroke);
			g2.drawArc(10, 10, getWidth() - 20, getHeight() - 20, counter, length());
		}
	}
}
