package ca.wise.manager.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import ca.hss.tr.Translated;
import ca.wise.lib.MqttSettings;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class MqttDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;
	private JTextField txtHost;
	private JTextField txtPort;
	private JTextField txtTopic;
	private JTextField txtUser;
	private JTextField txtPassword;
	private JCheckBox chkUser;
	private JLabel lblHost;
	private JLabel lblPassword;
	private int result = JOptionPane.CANCEL_OPTION;

	public MqttDlg(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi();
		setLocationRelativeTo(parent);
		populateValues();
	}
	
	public int getResult() {
		return result;
	}
	
	private void populateValues() {
		txtHost.setText(MqttSettings.getHost());
		txtPort.setText(String.valueOf(MqttSettings.getPort()));
		txtTopic.setText(MqttSettings.getTopic());
		txtUser.setText(MqttSettings.getUser());
		txtPassword.setText(MqttSettings.getPassword());
		chkUser.setSelected(MqttSettings.useAuthentication());
		txtUser.setEnabled(chkUser.isSelected());
		lblPassword.setEnabled(chkUser.isSelected());
		txtPassword.setEnabled(chkUser.isSelected());
	}
	
	private void verifyAndClose(ActionEvent e) {
	    MqttSettings.snapshot();
		MqttSettings.setHost(txtHost.getText());
		try {
			Integer port = Integer.parseInt(txtPort.getText());
			MqttSettings.setPort(port);
		}
		catch (NumberFormatException ex) { }
		MqttSettings.setUseAuthentication(chkUser.isSelected());
		MqttSettings.setTopic(txtTopic.getText());
		MqttSettings.setUser(txtUser.getText());
		MqttSettings.setPassword(txtPassword.getText());
		
		result = JOptionPane.OK_OPTION;
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(316, 219));
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 0;
		panel_1.add(panel_3, gbc_panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new TitledBorder(null, getResources().getString("server.settings"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_3.add(panel_5, BorderLayout.CENTER);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[]{0, 0, 0};
		gbl_panel_5.rowHeights = new int[]{30, 30, 30, 30, 30, 0};
		gbl_panel_5.columnWeights = new double[]{0.0, 1.0, 0.0};
		gbl_panel_5.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_5.setLayout(gbl_panel_5);
		
		lblHost = new JLabel(getResources().getString("host"));
		GridBagConstraints gbc_lblHost = new GridBagConstraints();
		gbc_lblHost.anchor = GridBagConstraints.EAST;
		gbc_lblHost.insets = new Insets(0, 5, 0, 5);
		gbc_lblHost.gridx = 0;
		gbc_lblHost.gridy = 0;
		panel_5.add(lblHost, gbc_lblHost);
		
		txtHost = new JTextField();
		GridBagConstraints gbc_txtHost = new GridBagConstraints();
		gbc_txtHost.weightx = 1.0;
		gbc_txtHost.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtHost.gridx = 1;
		gbc_txtHost.gridy = 0;
		panel_5.add(txtHost, gbc_txtHost);
		txtHost.setColumns(10);
		
		JLabel lblPort = new JLabel(getResources().getString("port"));
		GridBagConstraints gbc_lblPort = new GridBagConstraints();
		gbc_lblPort.anchor = GridBagConstraints.EAST;
		gbc_lblPort.insets = new Insets(0, 5, 0, 5);
		gbc_lblPort.gridx = 0;
		gbc_lblPort.gridy = 1;
		panel_5.add(lblPort, gbc_lblPort);
		
		txtPort = new JTextField();
		GridBagConstraints gbc_txtPort = new GridBagConstraints();
		gbc_txtPort.weightx = 1.0;
		gbc_txtPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPort.gridx = 1;
		gbc_txtPort.gridy = 1;
		panel_5.add(txtPort, gbc_txtPort);
		txtPort.setColumns(10);
		
		JLabel lblTopic = new JLabel(getResources().getString("topic"));
		GridBagConstraints gbc_lblTopic = new GridBagConstraints();
		gbc_lblTopic.anchor = GridBagConstraints.EAST;
		gbc_lblTopic.insets = new Insets(0, 5, 0, 5);
		gbc_lblTopic.gridx = 0;
		gbc_lblTopic.gridy = 2;
		panel_5.add(lblTopic, gbc_lblTopic);
		
		txtTopic = new JTextField();
		GridBagConstraints gbc_txtTopic = new GridBagConstraints();
		gbc_txtTopic.weightx = 1.0;
		gbc_txtTopic.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtTopic.gridx = 1;
		gbc_txtTopic.gridy = 2;
		panel_5.add(txtTopic, gbc_txtTopic);
		txtTopic.setColumns(10);
		
		chkUser = new JCheckBox(getResources().getString("user"));
		GridBagConstraints gbc_chkUser = new GridBagConstraints();
		gbc_chkUser.anchor = GridBagConstraints.EAST;
		gbc_chkUser.insets = new Insets(0, 5, 0, 5);
		gbc_chkUser.gridx = 0;
		gbc_chkUser.gridy = 3;
		chkUser.addActionListener(e -> {
			txtUser.setEnabled(chkUser.isSelected());
			lblPassword.setEnabled(chkUser.isSelected());
			txtPassword.setEnabled(chkUser.isSelected());
		});
		panel_5.add(chkUser, gbc_chkUser);
		
		txtUser = new JTextField();
		txtUser.setEnabled(false);
		GridBagConstraints gbc_txtUser = new GridBagConstraints();
		gbc_txtUser.weightx = 1.0;
		gbc_txtUser.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtUser.gridx = 1;
		gbc_txtUser.gridy = 3;
		panel_5.add(txtUser, gbc_txtUser);
		txtUser.setColumns(10);
		
		lblPassword = new JLabel(getResources().getString("password"));
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.anchor = GridBagConstraints.EAST;
		gbc_lblPassword.insets = new Insets(0, 5, 0, 5);
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 4;
		panel_5.add(lblPassword, gbc_lblPassword);
		
		txtPassword = new JTextField();
		GridBagConstraints gbc_txtPassword = new GridBagConstraints();
		gbc_txtPassword.weightx = 1.0;
		gbc_txtPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPassword.gridx = 1;
		gbc_txtPassword.gridy = 4;
		panel_5.add(txtPassword, gbc_txtPassword);
		txtPassword.setColumns(10);
		
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets(0, 5, 5, 5);
		panel_1.add(panel, gbc_panel);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		panel.add(panel_2, BorderLayout.EAST);
		
		JButton btnSave = new JButton(getResources().getString("ok"));
		btnSave.addActionListener(this::verifyAndClose);
		panel_2.add(btnSave);
		
		JButton btnCancel = new JButton(getResources().getString("cancel"));
		btnCancel.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		panel_2.add(btnCancel);
		
		pack();
		Dimension dim = getSize();
		dim.width += 50;
		dim.height += 10;
		setMinimumSize(dim);
	}
}
