package ca.wise.manager.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import ca.hss.platform.OperatingSystem;
import ca.hss.tr.Translated;
import ca.wise.lib.Settings;
import lombok.Getter;

public class BuilderSettingsDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;
	private int result = JOptionPane.CANCEL_OPTION;
	
	@Getter private boolean builderLocationChanged = false;
	@Getter private boolean logLevelChanged = false;
	@Getter private boolean outputTypeChanged = false;
	@Getter private boolean jobDirectoryChanged = false;
	@Getter private boolean startAtStartChanged = false;
	
	private JTextField txtBuilderLocation;
	private JComboBox<String> cmbLogLevel;
	private JComboBox<String> cmbOutputType;
	private JTextField txtJobDirectory;
	private JCheckBox chkStartAtStart;

	public BuilderSettingsDlg(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi();
		setLocationRelativeTo(parent);
		
		txtBuilderLocation.setText(Settings.getBuilderLocation());
		
		cmbLogLevel.addItem(getResources().getString("log.warn"));
		if (Settings.getBuilderLogLevel().equals("warn"))
			cmbLogLevel.setSelectedIndex(0);
		cmbLogLevel.addItem(getResources().getString("log.severe"));
		if (Settings.getBuilderLogLevel().equals("severe"))
			cmbLogLevel.setSelectedIndex(1);
		cmbLogLevel.addItem(getResources().getString("log.info"));
		if (Settings.getBuilderLogLevel().equals("info"))
			cmbLogLevel.setSelectedIndex(2);
		cmbLogLevel.addItem(getResources().getString("log.off"));
		if (Settings.getBuilderLogLevel().equals("off"))
			cmbLogLevel.setSelectedIndex(3);
		cmbLogLevel.addItem(getResources().getString("log.all"));
		if (Settings.getBuilderLogLevel().equals("all"))
			cmbLogLevel.setSelectedIndex(4);
		
		int offset = 0;
		if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows) {
			cmbOutputType.addItem(getResources().getString("output.xml"));
			if (Settings.getBuilderOutputType().equals("xml"))
				cmbOutputType.setSelectedIndex(0);
			offset++;
		}
		cmbOutputType.addItem(getResources().getString("output.json"));
		if (Settings.getBuilderOutputType().equals("json"))
			cmbOutputType.setSelectedIndex(offset);
		cmbOutputType.addItem(getResources().getString("output.minjson"));
		if (Settings.getBuilderOutputType().equals("json_mini"))
			cmbOutputType.setSelectedIndex(offset + 1);
		cmbOutputType.addItem(getResources().getString("output.binary"));
		if (Settings.getBuilderOutputType().equals("binary"))
			cmbOutputType.setSelectedIndex(offset + 2);
		
		txtJobDirectory.setText(Settings.getJobDirectory());
		
		cmbLogLevel.addItemListener(e -> logLevelChanged = true);
		cmbOutputType.addItemListener(e -> outputTypeChanged = true);
		
		chkStartAtStart.setSelected(Settings.getBuilderStartAtStart());
		chkStartAtStart.addItemListener(e -> startAtStartChanged = true);
	}
	
	private void save(ActionEvent event) {
	    Settings.snapshot();
		if (builderLocationChanged)
			Settings.setBuilderLocation(txtBuilderLocation.getText());
		if (jobDirectoryChanged)
			Settings.setJobDirectory(txtJobDirectory.getText());
		if (logLevelChanged) {
			String level;
			switch (cmbLogLevel.getSelectedIndex()) {
			case 1:
				level = "severe";
				break;
			case 2:
				level = "info";
				break;
			case 3:
				level = "off";
				break;
			case 4:
				level = "all";
				break;
			default:
				level = "warn";
				break;
			}
			Settings.setBuilderLogLevel(level);
		}
		if (outputTypeChanged) {
			int index = cmbOutputType.getSelectedIndex();
			if (cmbOutputType.getItemCount() < 4)
				index++;
			String type;
			switch (index) {
			case 1:
				type = "json";
				break;
			case 2:
				type = "json_mini";
				break;
			case 3:
				type = "binary";
				break;
			default:
				type = "xml";
				break;
			}
			Settings.setBuilderOutputType(type);
		}
		if (startAtStartChanged)
			Settings.setBuilderStartAtStart(chkStartAtStart.isSelected());
		result = JOptionPane.OK_OPTION;
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	private void selectJobDirectory(ActionEvent event) {
		String jobDir = Settings.getJobDirectory();
		JFileChooser chooser = new JFileChooser();
		if (jobDir != null && jobDir.length() > 0)
			chooser.setCurrentDirectory(new File(jobDir));
		chooser.setDialogTitle(getResources().getString("select.job.dir"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			txtJobDirectory.setText(chooser.getSelectedFile().getAbsolutePath());
			jobDirectoryChanged = true;
		}
	}
	
	private void selectWiseBuilder(ActionEvent event) {
		String current = Settings.getBuilderLocation();
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.jar"), "jar"));
		chooser.setDialogTitle(getResources().getString("select.wise.exe"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setCurrentDirectory(new File(current));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			txtBuilderLocation.setText(chooser.getSelectedFile().getAbsolutePath());
			builderLocationChanged = true;
		}
	}
	
	/**
	 * The result of the dialog.
	 * @return Either {@link JOptionPane#CANCEL_OPTION} or {@link JOptionPane#OK_OPTION}.
	 */
	public int getResult() { return result; }
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(461, 200));
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		panel.add(panel_2, BorderLayout.EAST);
		
		JButton btnSave = new JButton(getResources().getString("save"));
		btnSave.addActionListener(this::save);
		panel_2.add(btnSave);
		
		JButton btnCancel = new JButton(getResources().getString("cancel"));
		btnCancel.addActionListener(e -> {
			this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
		panel_2.add(btnCancel);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel_3, BorderLayout.CENTER);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{0, 0, 0};
		gbl_panel_3.rowHeights = new int[]{30, 30, 30, 30, 30, 0};
		gbl_panel_3.columnWeights = new double[]{0.0, 1.0, 0.0};
		gbl_panel_3.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_3.setLayout(gbl_panel_3);
		
		JLabel lblLocation = new JLabel(getResources().getString("entry.location"));
		GridBagConstraints gbc_lblLocation = new GridBagConstraints();
		gbc_lblLocation.anchor = GridBagConstraints.EAST;
		gbc_lblLocation.insets = new Insets(0, 5, 0, 5);
		gbc_lblLocation.gridx = 0;
		gbc_lblLocation.gridy = 0;
		panel_3.add(lblLocation, gbc_lblLocation);
		
		JPanel panel_4 = new JPanel();
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.weightx = 1.0;
		gbc_panel_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_4.gridx = 1;
		gbc_panel_4.gridy = 0;
		panel_3.add(panel_4, gbc_panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{0, 0, 0};
		gbl_panel_4.rowHeights = new int[]{30, 0};
		gbl_panel_4.columnWeights = new double[]{1.0, 0.0, 0.0};
		gbl_panel_4.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_4.setLayout(gbl_panel_4);
		
		txtBuilderLocation = new JTextField();
		txtBuilderLocation.setEditable(false);
		GridBagConstraints gbc_txtBuilderLocation = new GridBagConstraints();
		gbc_txtBuilderLocation.weightx = 1.0;
		gbc_txtBuilderLocation.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtBuilderLocation.gridx = 0;
		gbc_txtBuilderLocation.gridy = 0;
		panel_4.add(txtBuilderLocation, gbc_txtBuilderLocation);
		
		JButton btnSelectBuilderLocation = new JButton("...");
		btnSelectBuilderLocation.addActionListener(this::selectWiseBuilder);
		GridBagConstraints gbc_btnSelectBuilderLocation = new GridBagConstraints();
		gbc_btnSelectBuilderLocation.gridx = 1;
		gbc_btnSelectBuilderLocation.gridy = 0;
		panel_4.add(btnSelectBuilderLocation, gbc_btnSelectBuilderLocation);
		
		JLabel lblLogLevel = new JLabel(getResources().getString("entry.log"));
		GridBagConstraints gbc_lblLogLevel = new GridBagConstraints();
		gbc_lblLogLevel.anchor = GridBagConstraints.EAST;
		gbc_lblLogLevel.insets = new Insets(0, 5, 0, 5);
		gbc_lblLogLevel.gridx = 0;
		gbc_lblLogLevel.gridy = 1;
		panel_3.add(lblLogLevel, gbc_lblLogLevel);
		
		cmbLogLevel = new JComboBox<>();
		GridBagConstraints gbc_cmbLogLevel = new GridBagConstraints();
		gbc_cmbLogLevel.weightx = 1.0;
		gbc_cmbLogLevel.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbLogLevel.gridx = 1;
		gbc_cmbLogLevel.gridy = 1;
		panel_3.add(cmbLogLevel, gbc_cmbLogLevel);
		
		JLabel lblOutputType = new JLabel(getResources().getString("entry.output"));
		GridBagConstraints gbc_lblOutputType = new GridBagConstraints();
		gbc_lblOutputType.anchor = GridBagConstraints.EAST;
		gbc_lblOutputType.insets = new Insets(0, 5, 0, 5);
		gbc_lblOutputType.gridx = 0;
		gbc_lblOutputType.gridy = 2;
		panel_3.add(lblOutputType, gbc_lblOutputType);
		
		cmbOutputType = new JComboBox<>();
		GridBagConstraints gbc_cmbOutputType = new GridBagConstraints();
		gbc_cmbOutputType.weightx = 1.0;
		gbc_cmbOutputType.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbOutputType.gridx = 1;
		gbc_cmbOutputType.gridy = 2;
		panel_3.add(cmbOutputType, gbc_cmbOutputType);
		
		JLabel lblJobDirectory = new JLabel(getResources().getString("entry.directory"));
		GridBagConstraints gbc_lblJobDirectory = new GridBagConstraints();
		gbc_lblJobDirectory.anchor = GridBagConstraints.EAST;
		gbc_lblJobDirectory.insets = new Insets(0, 5, 0, 5);
		gbc_lblJobDirectory.gridx = 0;
		gbc_lblJobDirectory.gridy = 3;
		panel_3.add(lblJobDirectory, gbc_lblJobDirectory);
		
		JPanel panel_5 = new JPanel();
		GridBagConstraints gbc_panel_5 = new GridBagConstraints();
		gbc_panel_5.weightx = 1.0;
		gbc_panel_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_5.gridx = 1;
		gbc_panel_5.gridy = 3;
		panel_3.add(panel_5, gbc_panel_5);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[]{0, 0, 0};
		gbl_panel_5.rowHeights = new int[]{30, 0};
		gbl_panel_5.columnWeights = new double[]{1.0, 0.0, 0.0};
		gbl_panel_5.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_5.setLayout(gbl_panel_5);
		
		txtJobDirectory = new JTextField();
		txtJobDirectory.setEditable(false);
		GridBagConstraints gbc_txtJobDirectory = new GridBagConstraints();
		gbc_txtJobDirectory.weightx = 1.0;
		gbc_txtJobDirectory.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtJobDirectory.gridx = 0;
		gbc_txtJobDirectory.gridy = 0;
		panel_5.add(txtJobDirectory, gbc_txtJobDirectory);
		
		JButton btnSelectJobDirectory = new JButton("...");
		btnSelectJobDirectory.addActionListener(this::selectJobDirectory);
		GridBagConstraints gbc_btnSelectJobDirectory = new GridBagConstraints();
		gbc_btnSelectJobDirectory.gridx = 1;
		gbc_btnSelectJobDirectory.gridy = 0;
		panel_5.add(btnSelectJobDirectory, gbc_btnSelectJobDirectory);
		
		JLabel lblStartAtStart = new JLabel(getResources().getString("start.start"));
		GridBagConstraints gbc_lblStartAtStart = new GridBagConstraints();
		gbc_lblStartAtStart.anchor = GridBagConstraints.EAST;
		gbc_lblStartAtStart.insets = new Insets(0, 5, 0, 5);
		gbc_lblStartAtStart.gridx = 0;
		gbc_lblStartAtStart.gridy = 4;
		panel_3.add(lblStartAtStart, gbc_lblStartAtStart);
		
		chkStartAtStart = new JCheckBox("");
		GridBagConstraints gbc_chkStartAtStart = new GridBagConstraints();
		gbc_chkStartAtStart.weightx = 1.0;
		gbc_chkStartAtStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkStartAtStart.gridx = 1;
		gbc_chkStartAtStart.gridy = 4;
		panel_3.add(chkStartAtStart, gbc_chkStartAtStart);
		
		pack();
		setMinimumSize(getSize());
	}
}
