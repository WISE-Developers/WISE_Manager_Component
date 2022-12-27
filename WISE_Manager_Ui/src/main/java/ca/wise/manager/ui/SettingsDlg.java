package ca.wise.manager.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import ca.hss.platform.OperatingSystem;
import ca.hss.tr.Translated;
import ca.wise.lib.CPUInfo;
import ca.wise.lib.Settings;
import lombok.Getter;

import javax.swing.JPanel;
import javax.swing.JSpinner;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import java.awt.Insets;
import java.awt.SystemTray;

public class SettingsDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;
	
	private JTextField txtJobDirectory;
	private JTextField txtWiseExe;
	private JCheckBox chkMinimize;
	private JCheckBox chkRestartjobs;
	private JCheckBox chkStartPaused;
	private JCheckBox chkLockCpu;
	private JCheckBox chkNativeLaf;
	private JCheckBox chkNumaLock;
	private JComboBox<String> cmbLanguage;
	private JSpinner spnBufferSize;
    private JSpinner spnProcessCount;
    private JSpinner spnSkipProcessCount;
	private JLabel lblRpcAddress;
	private JTextField txtRpcAddress;
	private JLabel lblRpcPort;
	private JSpinner txtRpcPort;
	private JCheckBox chkEnableRpc;
    private JCheckBox chkEnableShmem;
	
	@Getter private boolean jobDirectoryChanged = false;
	@Getter private boolean wiseExeChanged = false;
	@Getter private boolean minimizeTrayChanged = false;
	@Getter private boolean languageChanged = false;
	@Getter private boolean restartOldChanged = false;
	@Getter private boolean lockCpuChanged = false;
	@Getter private boolean nativeLafChanged = false;
	@Getter private boolean startPausedChanged = false;
	@Getter private boolean bufferSizeChanged = false;
	@Getter private boolean processCountChanged = false;
	@Getter private boolean skipProcessCountChanged = false;
	@Getter private boolean numaLockChanged = false;
	@Getter private boolean rpcChanged = false;
	@Getter private boolean enableShmemChanged = false;

	public SettingsDlg(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi();
		setLocationRelativeTo(parent);
		
		cmbLanguage.addItem(getResources().getString("language.english"));
		cmbLanguage.addItem(getResources().getString("language.french"));
		
		txtJobDirectory.setText(Settings.getJobDirectory());
		txtWiseExe.setText(Settings.getWiseExe());
		chkMinimize.setSelected(Settings.getMinmizeTray());
		if (Settings.getLanguage().equals("fr"))
			cmbLanguage.setSelectedIndex(1);
		else
			cmbLanguage.setSelectedItem(0);
		
		chkMinimize.addActionListener(e -> minimizeTrayChanged = true);
		cmbLanguage.addItemListener(e -> {
			if (!languageChanged) {
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(this, getResources().getString("language.restart"), getResources().getString("language.change"), JOptionPane.OK_OPTION);
				});
			}
			languageChanged = true;
		});

		chkRestartjobs.setSelected(Settings.getRestartOld());
		chkRestartjobs.addActionListener(this::changeRestart);
		
		chkStartPaused.setSelected(Settings.getStartPaused());
		chkStartPaused.addActionListener(this::changeStartPaused);
		
		chkLockCpu.setSelected(Settings.getLockCPU());
		chkLockCpu.addActionListener(this::changeHardAffinity);
        
		chkEnableShmem.setSelected(Settings.isRespectShmem());
		chkEnableShmem.addActionListener(this::changeShmem);
		
		chkNativeLaf.setSelected(Settings.getNativeLAF());
		chkNativeLaf.addActionListener(this::changeNativeLaf);
		
		spnBufferSize.setValue(Settings.getBuilderMaxBufferSize());
		
		spnProcessCount.setValue(Settings.getProcesses());
		spnSkipProcessCount.setValue(Settings.getSkipProcesses());
	}
	
	private void selectJobDirectory(ActionEvent e) {
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
	
	private void selectWiseExecutable(ActionEvent e) {
		String current = Settings.getWiseExe();
		JFileChooser chooser = new JFileChooser();
		if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Windows) {
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.exe"), "exe"));
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.script.windows"), "bat"));
		}
		else if (OperatingSystem.getOperatingSystem().getType() == OperatingSystem.Type.Mac)
			chooser.addChoosableFileFilter(new FileNameExtensionFilter(getResources().getString("files.app"), "app"));
		chooser.setDialogTitle(getResources().getString("select.wise.exe"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setCurrentDirectory(new File(current));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			txtWiseExe.setText(chooser.getSelectedFile().getAbsolutePath());
			wiseExeChanged = true;
		}
	}
	
	private void changeRestart(ActionEvent e) {
		restartOldChanged = true;
	}
	
	private void changeStartPaused(ActionEvent e) {
		startPausedChanged = true;
	}
	
	private void changeHardAffinity(ActionEvent e) {
		lockCpuChanged = true;
	}
	
	private void changeNativeLaf(ActionEvent e) {
		nativeLafChanged = true;
	}
	
	private void changeShmem(ActionEvent e) {
	    enableShmemChanged = true;
	}
	
	private void save(ActionEvent e) {
		boolean restartRequired = false;
		Settings.snapshot();
		if (jobDirectoryChanged)
			Settings.setJobDirectory(txtJobDirectory.getText());
		if (wiseExeChanged)
			Settings.setWiseExe(txtWiseExe.getText());
		if (minimizeTrayChanged)
			Settings.setMinimizeTray(chkMinimize.isSelected());
		if (languageChanged) {
			if (cmbLanguage.getSelectedIndex() == 1)
				Settings.setLanguage("fr");
			else
				Settings.setLanguage("en");
		}
		if (restartOldChanged)
			Settings.setRestartOld(chkRestartjobs.isSelected());
		if (startPausedChanged)
			Settings.setStartPaused(chkStartPaused.isSelected());
		if (lockCpuChanged) {
			boolean old = Settings.getLockCPU();
			Settings.setLockCPU(chkLockCpu.isSelected());
			if (old != Settings.getLockCPU())
				restartRequired = true;
		}
		if (enableShmemChanged)
		    Settings.setRespectShmem(chkEnableShmem.isSelected());
		if (nativeLafChanged) {
			boolean old = Settings.getNativeLAF();
			Settings.setNativeLAF(chkNativeLaf.isSelected());
			if (old != Settings.getNativeLAF())
				restartRequired = true;
		}
		if (bufferSizeChanged)
			Settings.setBuilderMaxBufferSize(((Number)spnBufferSize.getValue()).intValue());
		if (processCountChanged)
		    Settings.setProcesses(((Number)spnProcessCount.getValue()).intValue());
		if (skipProcessCountChanged)
		    Settings.setSkipProcesses(((Number)spnSkipProcessCount.getValue()).intValue());
		if (numaLockChanged)
		    Settings.setNumaLock(chkNumaLock.isSelected());
		if (rpcChanged) {
			Settings.setRpcEnabled(chkEnableRpc.isSelected());
			Settings.setRpcAddress(txtRpcAddress.getText());
			if (txtRpcPort.getValue() instanceof Number)
				Settings.setRpcPort(((Number)txtRpcPort.getValue()).intValue());
		}
		
		if (restartRequired) {
			JOptionPane.showMessageDialog(this, getResources().getString("settings.cpu.changed"), getResources().getString("message"), JOptionPane.INFORMATION_MESSAGE);
		}
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(461, 300));
		
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
			jobDirectoryChanged = false;
			wiseExeChanged = false;
			minimizeTrayChanged = false;
			languageChanged = false;
			this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
		panel_2.add(btnCancel);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{1.0, 1.0, 1.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 0;
		panel_1.add(panel_3, gbc_panel_3);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		boolean showNumaLock = CPUInfo.staticNumaCount() > 1;
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new TitledBorder(null, getResources().getString("settings.execution"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_3.add(panel_5, BorderLayout.CENTER);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[]{0, 0, 0};
		if (showNumaLock)
		    gbl_panel_5.rowHeights = new int[]{30, 30, 30, 30, 30, 30, 30, 30, 30, 0};
		else
            gbl_panel_5.rowHeights = new int[]{30, 30, 30, 30, 30, 30, 30, 30, 0};
		gbl_panel_5.columnWeights = new double[]{0.0, 1.0, 0.0};
		if (showNumaLock)
		    gbl_panel_5.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		else
            gbl_panel_5.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_5.setLayout(gbl_panel_5);
		
		JLabel lblNewLabel = new JLabel(getResources().getString("settings.jobs"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.insets = new Insets(0, 5, 0, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel_5.add(lblNewLabel, gbc_lblNewLabel);
		
		txtJobDirectory = new JTextField();
		txtJobDirectory.setEditable(false);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.weightx = 1.0;
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		panel_5.add(txtJobDirectory, gbc_textField);
		
		JButton btnSelectJobDirectory = new JButton("...");
		btnSelectJobDirectory.addActionListener(this::selectJobDirectory);
		GridBagConstraints gbc_btnSelectJobDirectory = new GridBagConstraints();
		gbc_btnSelectJobDirectory.insets = new Insets(0, 0, 0, 5);
		gbc_btnSelectJobDirectory.gridx = 2;
		gbc_btnSelectJobDirectory.gridy = 0;
		panel_5.add(btnSelectJobDirectory, gbc_btnSelectJobDirectory);
		
		JLabel lblNewLabel_1 = new JLabel(getResources().getString("settings.exe"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.insets = new Insets(0, 5, 0, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		panel_5.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		txtWiseExe = new JTextField();
		txtWiseExe.setEditable(false);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.weightx = 1.0;
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 1;
		gbc_textField_1.gridy = 1;
		panel_5.add(txtWiseExe, gbc_textField_1);
		
		JButton btnSelectWise = new JButton("...");
		btnSelectWise.addActionListener(this::selectWiseExecutable);
		GridBagConstraints gbc_btnSelectWise = new GridBagConstraints();
		gbc_btnSelectWise.insets = new Insets(0, 0, 0, 5);
		gbc_btnSelectWise.gridx = 2;
		gbc_btnSelectWise.gridy = 1;
		panel_5.add(btnSelectWise, gbc_btnSelectWise);
		
		JLabel lblRestartJobs = new JLabel(getResources().getString("settings.restart"));
		lblRestartJobs.setToolTipText(getResources().getString("settings.restart.desc"));
		GridBagConstraints gbc_lblRestartJobs = new GridBagConstraints();
		gbc_lblRestartJobs.anchor = GridBagConstraints.EAST;
		gbc_lblRestartJobs.insets = new Insets(0, 5, 0, 5);
		gbc_lblRestartJobs.gridx = 0;
		gbc_lblRestartJobs.gridy = 2;
		panel_5.add(lblRestartJobs, gbc_lblRestartJobs);
		
		chkRestartjobs = new JCheckBox();
		GridBagConstraints gbc_chkRestartJobs = new GridBagConstraints();
		gbc_chkRestartJobs.weightx = 1.0;
		gbc_chkRestartJobs.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkRestartJobs.gridx = 1;
		gbc_chkRestartJobs.gridy = 2;
		panel_5.add(chkRestartjobs, gbc_chkRestartJobs);
		
		JLabel lblStartPaused = new JLabel(getResources().getString("settings.paused"));
		lblStartPaused.setToolTipText(getResources().getString("settings.paused.desc"));
		GridBagConstraints gbc_lblStartPaused = new GridBagConstraints();
		gbc_lblStartPaused.anchor = GridBagConstraints.EAST;
		gbc_lblStartPaused.insets = new Insets(0, 5, 0, 5);
		gbc_lblStartPaused.gridx = 0;
		gbc_lblStartPaused.gridy = 3;
		panel_5.add(lblStartPaused, gbc_lblStartPaused);
		
		chkStartPaused = new JCheckBox();
		GridBagConstraints gbc_chkStartPaused = new GridBagConstraints();
		gbc_chkStartPaused.weightx = 1.0;
		gbc_chkStartPaused.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkStartPaused.gridx = 1;
		gbc_chkStartPaused.gridy = 3;
		panel_5.add(chkStartPaused, gbc_chkStartPaused);
		
		JLabel lblLockCPU = new JLabel(getResources().getString("settings.cpu.lock"));
		lblLockCPU.setToolTipText(getResources().getString("settings.cpu.lock.desc"));
		GridBagConstraints gbc_lblLockCPU = new GridBagConstraints();
		gbc_lblLockCPU.anchor = GridBagConstraints.EAST;
		gbc_lblLockCPU.insets = new Insets(0, 5, 0, 5);
		gbc_lblLockCPU.gridx = 0;
		gbc_lblLockCPU.gridy = 4;
		panel_5.add(lblLockCPU, gbc_lblLockCPU);
		
		chkLockCpu = new JCheckBox();
		GridBagConstraints gbc_chkLockCpu = new GridBagConstraints();
		gbc_chkLockCpu.weightx = 1.0;
		gbc_chkLockCpu.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkLockCpu.gridx = 1;
		gbc_chkLockCpu.gridy = 4;
		panel_5.add(chkLockCpu, gbc_chkLockCpu);
        
        JLabel lblEnableShmem = new JLabel(getResources().getString("settings.shmem"));
        lblEnableShmem.setToolTipText(getResources().getString("settings.shmem.desc"));
        GridBagConstraints gbc_lblEnableShmem = new GridBagConstraints();
        gbc_lblEnableShmem.anchor = GridBagConstraints.EAST;
        gbc_lblEnableShmem.insets = new Insets(0, 5, 0, 5);
        gbc_lblEnableShmem.gridx = 0;
        gbc_lblEnableShmem.gridy = 5;
        panel_5.add(lblEnableShmem, gbc_lblEnableShmem);
        
        chkEnableShmem = new JCheckBox();
        GridBagConstraints gbc_chkEnableShmem = new GridBagConstraints();
        gbc_chkEnableShmem.weightx = 1.0;
        gbc_chkEnableShmem.fill = GridBagConstraints.HORIZONTAL;
        gbc_chkEnableShmem.gridx = 1;
        gbc_chkEnableShmem.gridy = 5;
        panel_5.add(chkEnableShmem, gbc_chkEnableShmem);
		
		JLabel lblBufferSize = new JLabel(getResources().getString("out.buffer"));
		lblBufferSize.setToolTipText(getResources().getString("out.buffer.desc"));
		GridBagConstraints gbc_lblBufferSize = new GridBagConstraints();
		gbc_lblBufferSize.anchor = GridBagConstraints.EAST;
		gbc_lblBufferSize.insets = new Insets(0, 5, 0, 5);
		gbc_lblBufferSize.gridx = 0;
		gbc_lblBufferSize.gridy = 6;
		panel_5.add(lblBufferSize, gbc_lblBufferSize);
		
		spnBufferSize = new JSpinner();
		GridBagConstraints gbc_spnBufferSize = new GridBagConstraints();
		gbc_spnBufferSize.weightx = 1.0;
		gbc_spnBufferSize.gridwidth = GridBagConstraints.REMAINDER;
		gbc_spnBufferSize.fill = GridBagConstraints.HORIZONTAL;
		gbc_spnBufferSize.insets = new Insets(0, 0, 0, 5);
		gbc_spnBufferSize.gridx = 1;
		gbc_spnBufferSize.gridy = 6;
		panel_5.add(spnBufferSize, gbc_spnBufferSize);
		
		SpinnerModel model = new SpinnerNumberModel(Settings.getBuilderMaxBufferSize().intValue(), 128, 1073741824, 256);
		model.addChangeListener(e -> bufferSizeChanged = true);
		spnBufferSize.setModel(model);
		if (spnBufferSize.getEditor() instanceof JSpinner.DefaultEditor) {
			((JSpinner.DefaultEditor)spnBufferSize.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
		}
        
        JLabel lblProcessCount = new JLabel(getResources().getString("out.processes"));
        lblProcessCount.setToolTipText(getResources().getString("out.processes.desc"));
        GridBagConstraints gbc_lblProcessCount = new GridBagConstraints();
        gbc_lblProcessCount.anchor = GridBagConstraints.EAST;
        gbc_lblProcessCount.insets = new Insets(0, 5, 0, 5);
        gbc_lblProcessCount.gridx = 0;
        gbc_lblProcessCount.gridy = 7;
        panel_5.add(lblProcessCount, gbc_lblProcessCount);
        
        spnProcessCount = new JSpinner();
        GridBagConstraints gbc_spnProcessCount = new GridBagConstraints();
        gbc_spnProcessCount.weightx = 1.0;
        gbc_spnProcessCount.gridwidth = GridBagConstraints.REMAINDER;
        gbc_spnProcessCount.fill = GridBagConstraints.HORIZONTAL;
        gbc_spnProcessCount.insets = new Insets(0, 0, 0, 5);
        gbc_spnProcessCount.gridx = 1;
        gbc_spnProcessCount.gridy = 7;
        panel_5.add(spnProcessCount, gbc_spnProcessCount);
        
        model = new SpinnerNumberModel(Settings.getProcesses(), 1, 128, 1);
        model.addChangeListener(e -> processCountChanged = true);
        spnProcessCount.setModel(model);
        if (spnProcessCount.getEditor() instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)spnProcessCount.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        }
        
        chkNumaLock = new JCheckBox();
        chkNumaLock.setSelected(Settings.getNumaLock());
        if (showNumaLock) {
            JLabel lblNumaLock = new JLabel(getResources().getString("out.numa"));
            lblNumaLock.setToolTipText(getResources().getString("out.numa.desc"));
            GridBagConstraints gbc_lblNumaLock = new GridBagConstraints();
            gbc_lblNumaLock.anchor = GridBagConstraints.EAST;
            gbc_lblNumaLock.insets = new Insets(0, 5, 0, 5);
            gbc_lblNumaLock.gridx = 0;
            gbc_lblNumaLock.gridy = 8;
            panel_5.add(lblNumaLock, gbc_lblNumaLock);
            
            GridBagConstraints gbc_chkNumaLock = new GridBagConstraints();
            gbc_chkNumaLock.weightx = 1.0;
            gbc_chkNumaLock.gridwidth = GridBagConstraints.REMAINDER;
            gbc_chkNumaLock.fill = GridBagConstraints.HORIZONTAL;
            gbc_chkNumaLock.insets = new Insets(0, 0, 0, 5);
            gbc_chkNumaLock.gridx = 1;
            gbc_chkNumaLock.gridy = 8;
            panel_5.add(chkNumaLock, gbc_chkNumaLock);
            chkNumaLock.addActionListener(x -> numaLockChanged = true);
        }
        
        JLabel lblSkipProcessCount = new JLabel(getResources().getString("out.skip"));
        if (showNumaLock)
            lblSkipProcessCount.setToolTipText(getResources().getString("out.skip.desc2"));
        else
            lblSkipProcessCount.setToolTipText(getResources().getString("out.skip.desc"));
        GridBagConstraints gbc_lblSkipProcessCount = new GridBagConstraints();
        gbc_lblSkipProcessCount.anchor = GridBagConstraints.EAST;
        gbc_lblSkipProcessCount.insets = new Insets(0, 5, 0, 5);
        gbc_lblSkipProcessCount.gridx = 0;
        gbc_lblSkipProcessCount.gridy = showNumaLock ? 9 : 8;
        panel_5.add(lblSkipProcessCount, gbc_lblSkipProcessCount);
        
        spnSkipProcessCount = new JSpinner();
        GridBagConstraints gbc_spnSkipProcessCount = new GridBagConstraints();
        gbc_spnSkipProcessCount.weightx = 1.0;
        gbc_spnSkipProcessCount.gridwidth = GridBagConstraints.REMAINDER;
        gbc_spnSkipProcessCount.fill = GridBagConstraints.HORIZONTAL;
        gbc_spnSkipProcessCount.insets = new Insets(0, 0, 0, 5);
        gbc_spnSkipProcessCount.gridx = 1;
        gbc_spnSkipProcessCount.gridy = showNumaLock ? 9 : 8;
        panel_5.add(spnSkipProcessCount, gbc_spnSkipProcessCount);
        
        model = new SpinnerNumberModel(Settings.getSkipProcesses(), 0, 128, 1);
        model.addChangeListener(e -> skipProcessCountChanged = true);
        spnSkipProcessCount.setModel(model);
        if (spnSkipProcessCount.getEditor() instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)spnSkipProcessCount.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        }
		
		JPanel rpcWrapperPanel = new JPanel();
		rpcWrapperPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
		GridBagConstraints gbc_rpcWrapperPanel = new GridBagConstraints();
		gbc_rpcWrapperPanel.fill = GridBagConstraints.BOTH;
		gbc_rpcWrapperPanel.gridx = 0;
		gbc_rpcWrapperPanel.gridy = 1;
		panel_1.add(rpcWrapperPanel, gbc_rpcWrapperPanel);
		rpcWrapperPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel rpcPanel = new JPanel();
		rpcPanel.setBorder(new TitledBorder(null, getResources().getString("settings.rpc"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
		rpcWrapperPanel.add(rpcPanel, BorderLayout.CENTER);
		GridBagLayout gbl_rpcPanel = new GridBagLayout();
		gbl_rpcPanel.columnWidths = new int[]{0, 0};
		gbl_rpcPanel.rowHeights = new int[]{30, 30, 30, 0};
		gbl_rpcPanel.columnWeights = new double[]{0.0, 1.0};
		gbl_rpcPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		rpcPanel.setLayout(gbl_rpcPanel);
		
		JLabel lblEnableRpc = new JLabel(getResources().getString("settings.rpc.enable"));
		GridBagConstraints gbc_lblEnableRpc = new GridBagConstraints();
		gbc_lblEnableRpc.anchor = GridBagConstraints.EAST;
		gbc_lblEnableRpc.insets = new Insets(0, 5, 0, 5);
		gbc_lblEnableRpc.gridx = 0;
		gbc_lblEnableRpc.gridy = 0;
		rpcPanel.add(lblEnableRpc, gbc_lblEnableRpc);
		
		chkEnableRpc = new JCheckBox();
		chkEnableRpc.setSelected(Settings.isRpcEnabled());
		chkEnableRpc.addActionListener(e -> {
			rpcChanged = true;
			if (chkEnableRpc.isSelected()) {
				lblRpcAddress.setEnabled(true);
				txtRpcAddress.setEnabled(true);
				lblRpcPort.setEnabled(true);
				txtRpcPort.setEnabled(true);
			}
			else {
				lblRpcAddress.setEnabled(false);
				txtRpcAddress.setEnabled(false);
				lblRpcPort.setEnabled(false);
				txtRpcPort.setEnabled(false);
			}
		});
		GridBagConstraints gbc_chkEnableRpc = new GridBagConstraints();
		gbc_chkEnableRpc.insets = new Insets(0, 0, 0, 5);
		gbc_chkEnableRpc.weightx = 1.0;
		gbc_chkEnableRpc.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkEnableRpc.gridx = 1;
		gbc_chkEnableRpc.gridy = 0;
		rpcPanel.add(chkEnableRpc, gbc_chkEnableRpc);
		
		lblRpcAddress = new JLabel(getResources().getString("settings.rpc.address"));
		lblRpcAddress.setEnabled(Settings.isRpcEnabled());
		GridBagConstraints gbc_lblRpcAddress = new GridBagConstraints();
		gbc_lblRpcAddress.anchor = GridBagConstraints.EAST;
		gbc_lblRpcAddress.insets = new Insets(0, 5, 0, 5);
		gbc_lblRpcAddress.gridx = 0;
		gbc_lblRpcAddress.gridy = 1;
		rpcPanel.add(lblRpcAddress, gbc_lblRpcAddress);
		
		txtRpcAddress = new JTextField();
		txtRpcAddress.setText(Settings.getRpcAddress());
		txtRpcAddress.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				rpcChanged = true;
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				rpcChanged = true;
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				rpcChanged = true;
			}
		});
		txtRpcAddress.setEnabled(Settings.isRpcEnabled());
		GridBagConstraints gbc_txtRpcAddress = new GridBagConstraints();
		gbc_txtRpcAddress.insets = new Insets(0, 0, 0, 5);
		gbc_txtRpcAddress.weightx = 1.0;
		gbc_txtRpcAddress.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtRpcAddress.gridx = 1;
		gbc_txtRpcAddress.gridy = 1;
		rpcPanel.add(txtRpcAddress, gbc_txtRpcAddress);
		
		lblRpcPort = new JLabel(getResources().getString("settings.rpc.port"));
		lblRpcPort.setEnabled(Settings.isRpcEnabled());
		GridBagConstraints gbc_lblRpcPort = new GridBagConstraints();
		gbc_lblRpcPort.anchor = GridBagConstraints.EAST;
		gbc_lblRpcPort.insets = new Insets(0, 5, 0, 5);
		gbc_lblRpcPort.gridx = 0;
		gbc_lblRpcPort.gridy = 2;
		rpcPanel.add(lblRpcPort, gbc_lblRpcPort);
		
		txtRpcPort = new JSpinner();
		txtRpcPort.setEnabled(Settings.isRpcEnabled());
		GridBagConstraints gbc_txtRpcPort = new GridBagConstraints();
		gbc_txtRpcPort.insets = new Insets(0, 0, 0, 5);
		gbc_txtRpcPort.weightx = 1.0;
		gbc_txtRpcPort.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtRpcPort.gridx = 1;
		gbc_txtRpcPort.gridy = 2;
		rpcPanel.add(txtRpcPort, gbc_txtRpcPort);
		
		model = new SpinnerNumberModel(Settings.getRpcPort(), 1, 100000, 1);
		model.addChangeListener(e -> rpcChanged = true);
		txtRpcPort.setModel(model);
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(txtRpcPort, "#");
		editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
		txtRpcPort.setEditor(editor);
		
		
		JPanel panel_4 = new JPanel();
		panel_4.setBorder(new EmptyBorder(0, 10, 10, 10));
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 0;
		gbc_panel_4.gridy = 2;
		panel_1.add(panel_4, gbc_panel_4);
		panel_4.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_6 = new JPanel();
		panel_6.setBorder(new TitledBorder(null, getResources().getString("settings.application"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_4.add(panel_6, BorderLayout.CENTER);
		GridBagLayout gbl_panel_6 = new GridBagLayout();
		gbl_panel_6.columnWidths = new int[]{0, 0};
		gbl_panel_6.rowHeights = new int[]{30, 30, 30, 0};
		gbl_panel_6.columnWeights = new double[]{0.0, 1.0};
		gbl_panel_6.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_6.setLayout(gbl_panel_6);
		
		JLabel lblNewLabel_2 = new JLabel(getResources().getString("settings.tray"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_2.insets = new Insets(0, 5, 0, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 0;
		panel_6.add(lblNewLabel_2, gbc_lblNewLabel_2);
		lblNewLabel_2.setEnabled(SystemTray.isSupported());
		
		chkMinimize = new JCheckBox();
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.insets = new Insets(0, 0, 0, 5);
		gbc_textField_2.weightx = 1.0;
		gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_2.gridx = 1;
		gbc_textField_2.gridy = 0;
		panel_6.add(chkMinimize, gbc_textField_2);
		chkMinimize.setEnabled(SystemTray.isSupported());
		
		JLabel lblNewLabel_3 = new JLabel(getResources().getString("settings.language"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_3.insets = new Insets(0, 5, 0, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 1;
		panel_6.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		cmbLanguage = new JComboBox<>();
		GridBagConstraints gbc_textField_3 = new GridBagConstraints();
		gbc_textField_3.insets = new Insets(0, 0, 0, 5);
		gbc_textField_3.weightx = 1.0;
		gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_3.gridx = 1;
		gbc_textField_3.gridy = 1;
		panel_6.add(cmbLanguage, gbc_textField_3);
		
		JLabel lblNativeLaf = new JLabel(getResources().getString("settings.native.laf"));
		GridBagConstraints gbc_lblNativeLaf = new GridBagConstraints();
		gbc_lblNativeLaf.anchor = GridBagConstraints.EAST;
		gbc_lblNativeLaf.insets = new Insets(0, 5, 0, 5);
		gbc_lblNativeLaf.gridx = 0;
		gbc_lblNativeLaf.gridy = 2;
		panel_6.add(lblNativeLaf, gbc_lblNativeLaf);
		
		chkNativeLaf = new JCheckBox();
		GridBagConstraints gbc_chkNativeLaf = new GridBagConstraints();
		gbc_chkNativeLaf.weightx = 1.0;
		gbc_chkNativeLaf.fill = GridBagConstraints.HORIZONTAL;
		gbc_chkNativeLaf.gridx = 1;
		gbc_chkNativeLaf.gridy = 2;
		panel_6.add(chkNativeLaf, gbc_chkNativeLaf);
		
		pack();
		setMinimumSize(getSize());
	}
}
