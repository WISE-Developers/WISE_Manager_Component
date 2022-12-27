package ca.wise.manager.ui;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import ca.hss.tr.Resources;
import ca.hss.tr.Translations;
import ca.wise.lib.JobLists;
import ca.wise.lib.mqtt.MqttMessage;
import lombok.Getter;
import lombok.Setter;

public class MqttMessageTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The maximum number of entries that will be displayed in the table.
	 */
	private static final int MAX_SIZE = 1000;
	
	private List<MqttMessage> messages = new ArrayList<>();
	private List<MqttMessage> toDisplay = new ArrayList<>();
	private Resources resources;
	private final JTable table;
	@Setter private JobLists jobs;
	@Getter @Setter private boolean autoScroll = true;
	@Getter private boolean onlyActive = false;
	@Getter private String filter = null;
	
	public MqttMessageTableModel(JTable table) {
		this.table = table;
		resources = Translations.getNamedResources("app");
	}
	
	public void addMessage(MqttMessage message) {
		synchronized(messages) {
			messages.add(message);
			//enforce the max length of the message list
			if (messages.size() > MAX_SIZE) {
			    //remove the last element in the list
			    MqttMessage removed = messages.remove(0);
			    //remove the element from the display list, if it is present
			    int index = toDisplay.indexOf(removed);
			    if (index >= 0) {
			        toDisplay.remove(index);
			        fireTableRowsDeleted(index, index);
			    }
			}
			if (satisfiesFilter(message.job)) {
				toDisplay.add(message);
				fireTableRowsInserted(toDisplay.size() - 1, toDisplay.size() - 1);
				SwingUtilities.invokeLater(() -> {
					if (autoScroll)
						table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
				});
			}
		}
	}
	
	/**
	 * Set whether or not only status messages from currently executing jobs should be displayed.
	 * This will override the custom filter.
	 * @param value True to only show messages from executing jobs.
	 */
	public void setOnlyActive(boolean value) {
		onlyActive = value;
		applyFilter();
	}
	
	/**
	 * Set a custom filter on the displayed messages.
	 * @param filter The filter to apply to the messages, null to clear the filter.
	 */
	public void setFilter(String filter) {
		this.filter = filter;
		applyFilter();
	}
	
	/**
	 * Clear all MQTT messages.
	 */
	public void clearMessages() {
		synchronized(messages) {
			messages.clear();
			toDisplay.clear();
			fireTableDataChanged();
		}
	}
	
	private boolean satisfiesFilter(String name) {
		if (onlyActive)
			return jobs.getUnfinishedJobs().contains(name);
		else if (filter != null && filter.length() > 0)
			return name.contains(filter);
		return true;
	}
	
	/**
	 * Apply the user requested filters to the displayed message list.
	 */
	private void applyFilter() {
		if (onlyActive) {
			List<String> names = jobs.getUnfinishedJobs();
			toDisplay = messages.stream().filter(x -> names.contains(x.job)).collect(Collectors.toList());
		}
		else if (filter != null && filter.length() > 0) {
			toDisplay = messages.stream().filter(x -> x.job.contains(filter)).collect(Collectors.toList());
		}
		else
			toDisplay = new ArrayList<>(messages);
		fireTableDataChanged();
		SwingUtilities.invokeLater(() -> {
			if (autoScroll)
				table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
		});
	}
	
	public void showMessageBody(int row, Window dlgParent) {
		JsonDlg dlg = new JsonDlg(dlgParent, toDisplay.get(row).message);
		dlg.setVisible(true);
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public int getRowCount() {
		return toDisplay.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return toDisplay.get(row).timestamp;
		case 1:
			return toDisplay.get(row).from;
		case 2:
			return toDisplay.get(row).job;
		case 3:
			return toDisplay.get(row).status;
		case 4:
			return toDisplay.get(row).topic;
		}
		return "";
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
	
	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return resources.getString("mqtt.arrival");
		case 1:
			return resources.getString("mqtt.from");
		case 2:
			return resources.getString("mqtt.job");
		case 3:
			return resources.getString("mqtt.status");
		case 4:
			return resources.getString("mqtt.topic");
		}
		return "";
	}
}
