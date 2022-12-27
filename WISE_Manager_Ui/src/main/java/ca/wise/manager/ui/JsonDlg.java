package ca.wise.manager.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JDialog;

import ca.hss.tr.Translated;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDlg extends JDialog implements Translated {
	private static final long serialVersionUID = 1L;

	public JsonDlg(Window parent, final String json) {
		super(parent, ModalityType.APPLICATION_MODAL);
		initializeUi(json);
		setLocationRelativeTo(parent);
	}
	
	private void initializeUi(final String json) {
		setTitle(getResources().getString("title"));
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(461, 588));
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(4, 10, 4, 10));
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JButton btnClose = new JButton(getResources().getString("close"));
		btnClose.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		panel.add(btnClose, BorderLayout.EAST);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		String pretty = json;
		try {
			ObjectMapper mapper = new ObjectMapper();
			Object raw = mapper.readValue(json, Object.class);
			pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(raw);
		}
		catch (IOException e) { }
		
		RSyntaxTextArea textArea = new RSyntaxTextArea();
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		textArea.setCodeFoldingEnabled(false);
		textArea.setText(pretty);
		RTextScrollPane scrollPane = new RTextScrollPane(textArea);
		panel_1.add(scrollPane, BorderLayout.CENTER);
	}
}
