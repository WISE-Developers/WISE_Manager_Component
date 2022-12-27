package ca.wise.manager.ui;

import ca.hss.tr.Translated;
import ca.hss.tr.Translations;

import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class AboutDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;

	public AboutDlg(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi();
		setLocationRelativeTo(parent);
	}
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		getContentPane().setLayout(null);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		setSize(300, 267);
		
		JLabel panel = new JLabel();
		panel.setBounds((getWidth() - 192) / 2, 12, 192, 149);
		panel.setIcon(new ImageIcon(getClass().getResource("/Prometheus-Logo.png")));
		getContentPane().add(panel);
		
		JButton btnClose = new JButton(getResources().getString("close"));
		btnClose.setBounds(201, 197, 75, 23);
		btnClose.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		getContentPane().add(btnClose);
		
		JLabel lblVersion = new JLabel();
		lblVersion.setHorizontalAlignment(SwingConstants.CENTER);
		lblVersion.setBounds(panel.getLocation().x, 164, 192, 13);
		lblVersion.setText(getResources().getString("version", BuildConfig.version.getOriginalString().substring(2)));
		getContentPane().add(lblVersion);
		
		JLabel lblDate = new JLabel();
		lblDate.setHorizontalAlignment(SwingConstants.CENTER);
		lblDate.setBounds(panel.getLocation().x, 177, 192, 13);
		lblDate.setText(getResources().getString("date", Translations.getNamedResources("app").getString("release.date")));
		getContentPane().add(lblDate);
		
		JButton btnLicense = new JButton(getResources().getString("licenses"));
		btnLicense.addActionListener(e -> {
			LicenseDlg dlg = new LicenseDlg(this);
			dlg.setVisible(true);
		});
		btnLicense.setBounds(10, 197, 75, 23);
		getContentPane().add(btnLicense);
	}
}
