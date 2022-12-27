package ca.wise.manager.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JDialog;

import ca.hss.tr.Translated;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Desktop;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.ListSelectionModel;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

public class LicenseDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;
	
	private LicenseData[] licenses = new LicenseData[] {
			new LicenseData("library.rsyntax", "license.bsd.modified", "/rsyntax.txt", "http://bobbylight.github.io/RSyntaxTextArea/"),
			new LicenseData("library.cli", "license.apache", "/apache2.txt", "http://commons.apache.org/proper/commons-cli/"),
			new LicenseData("library.hssjava", "license.apache", "/apache2.txt", "https://github.com/HeartlandSoftware/HSS_Java"),
			new LicenseData("library.jackson", "license.apache", "/apache2.txt", "https://github.com/FasterXML/jackson-core"),
			new LicenseData("library.log", "license.apache", "/apache2.txt", "https://logging.apache.org/log4j/2.x/"),
			new LicenseData("library.compress", "license.apache", "/apache2.txt", "https://commons.apache.org/proper/commons-compress/"),
			new LicenseData("library.mqtt", "license.eclipse", "/epl.txt", "https://www.eclipse.org/paho/"),
			new LicenseData("library.jna", "license.apache", "/apache2.txt", "https://github.com/java-native-access/jna"),
			new LicenseData("library.oshi", "license.eclipse", "/epl.txt", "https://github.com/oshi/oshi"),
			new LicenseData("library.version", "license.apache", "/apache2.txt", "https://github.com/G00fY2/version-compare"),
			new LicenseData("library.moquette", "license.eclipse", "/epl.txt", "https://projects.eclipse.org/projects/iot.moquette")
	};

	public LicenseDlg(Window parent) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi();
		setLocationRelativeTo(parent);
		
		DefaultListModel<LicenseData> model = new DefaultListModel<>();
		for (LicenseData data : licenses) {
			model.addElement(data);
		}
		list.setModel(model);
		list.setSelectedIndex(0);
	}
	
	private void listItemSelected(ListSelectionEvent e) {
		LicenseData data = licenses[list.getSelectedIndex()];
		lblTitle.setText(getResources().getString(data.titleId));
		lblWebsite.setText(data.website);
		String loc = data.licenseLocation;
		try {
			editorPane.setPage(getClass().getResource(loc));
			btnSave.setEnabled(true);
		} catch (IOException e2) { }
	}
	
	private void save(ActionEvent e) {
		String file = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setDialogTitle(getResources().getString("export"));
		chooser.setMultiSelectionEnabled(false);
		chooser.setAcceptAllFileFilterUsed(false);
		FileFilter filter = new FileNameExtensionFilter(getResources().getString("file.txt"), "txt");
		chooser.setFileFilter(filter);
		int retval = chooser.showSaveDialog(this);
		if (retval == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile().getAbsolutePath();
			if (!file.endsWith(".txt"))
				file += ".txt";
			try (FileOutputStream out = new FileOutputStream(file, false)) {
				out.write(editorPane.getText().getBytes());
				out.close();
			}
			catch (IOException e1) {
			}
		}
	}
	
	private JLabel lblTitle;
	private JLinkLabel lblWebsite;
	private JEditorPane editorPane;
	private JButton btnSave;
	private JList<LicenseData> list;
	
	private void initializeUi() {
		setTitle(getResources().getString("title"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(400, 534));
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(4, 10, 4, 10));
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JButton btnClose = new JButton(getResources().getString("close"));
		btnClose.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		panel.add(btnClose, BorderLayout.EAST);
		
		btnSave = new JButton(getResources().getString("save"));
		btnSave.addActionListener(this::save);
		btnSave.setEnabled(false);
		panel.add(btnSave, BorderLayout.WEST);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_4 = new JPanel();
		panel_2.add(panel_4, BorderLayout.SOUTH);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{10, 0};
		gbl_panel_4.rowHeights = new int[] {14, 14, 0};
		gbl_panel_4.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_4.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_4.setLayout(gbl_panel_4);
		
		lblWebsite = new JLinkLabel();
		lblWebsite.addLinkClickedListener(l -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(l));
				}
				catch (IOException|URISyntaxException e1) { }
			}
		});
		GridBagConstraints gbc_lblWebsite = new GridBagConstraints();
		gbc_lblWebsite.weightx = 1.0;
		gbc_lblWebsite.gridx = 0;
		gbc_lblWebsite.gridy = 0;
		panel_4.add(lblWebsite, gbc_lblWebsite);
		
		lblTitle = new JLabel();
		GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.weightx = 1.0;
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 1;
		panel_4.add(lblTitle, gbc_lblTitle);
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel_2.add(panel_5, BorderLayout.CENTER);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[]{1, 0};
		gbl_panel_5.rowHeights = new int[] {75, 0};
		gbl_panel_5.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_5.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_5.setLayout(gbl_panel_5);
		
		list = new JList<>();
		list.addListSelectionListener(this::listItemSelected);
		GridBagConstraints gbc_list = new GridBagConstraints();
		gbc_list.weightx = 1.0;
		gbc_list.fill = GridBagConstraints.HORIZONTAL;
		gbc_list.gridx = 0;
		gbc_list.gridy = 0;
		panel_5.add(list, gbc_list);
		list.setVisibleRowCount(4);
		list.setMinimumSize(new Dimension(300, 50));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		
		JScrollPane scrollPane = new JScrollPane(editorPane);
		panel_3.add(scrollPane, BorderLayout.CENTER);
	}

	private class LicenseData {
		public String nameId;
		public String titleId;
		public String licenseLocation;
		public String website;

		public LicenseData(String nameId, String titleId, String licenseLocation, String website) {
			this.nameId = nameId;
			this.titleId = titleId;
			this.licenseLocation = licenseLocation;
			this.website = website;
		}
		
		@Override
		public String toString() {
			return getResources().getString(nameId);
		}
	}
}
