package ca.wise.manager.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.hss.tr.Resources;
import ca.hss.tr.Translations;
import ca.wise.lib.Job;
import ca.wise.lib.Job.IPropertyChangedListener;
import ca.wise.lib.JobLists;
import ca.wise.lib.WISELogger;
import ca.wise.lib.JobLists.IListChangedListener;
import ca.wise.lib.WISELogger.LogName;

public class FinishedJobsTableModel extends AbstractTableModel implements IListChangedListener, IPropertyChangedListener {
	private static final long serialVersionUID = 1L;
	
	private JobLists jobs;
	private Resources resources;
	
	public FinishedJobsTableModel(JobLists jobs) {
		this.jobs = jobs;
		this.jobs.addFinishedListChangeListener(this);
		resources = Translations.getNamedResources("app");
	}
	
	public void doubleClick(int row) {
		if (row >= 0 && row < jobs.getFinishedJobs().size()) {
			Job job = jobs.getFinishedJobs().get(row);
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(job.getXmlPath().getParent().toFile());
				}
				catch (IOException e) {
					WISELogger.getSpecial(LogName.Ui).warn("Unable to open file manager.", e);
				}
			}
		}
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public int getRowCount() {
		return jobs.getFinishedJobs().size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		List<Job> data = jobs.getFinishedJobs();
		switch (col) {
		case 0:
			return data.get(row).getName();
		case 1:
			return data.get(row).getStart();
		case 2:
			return data.get(row).getEnd();
		case 3:
			return data.get(row).getStatus().toString();
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
			return resources.getString("job.name");
		case 1:
			return resources.getString("job.start");
		case 2:
			return resources.getString("job.end");
		case 3:
			return resources.getString("job.status");
		}
		return "";
	}

	@Override
	public void itemAdded(Job job, int index) {
		job.addPropertyChangedEventListener(this);
		fireTableRowsInserted(index, index);
	}

	@Override
	public void itemRemoved(Job job, int index) {
		job.removePropertyChangedEventListener(this);
		fireTableRowsDeleted(index, index);
	}

	@Override
	public void onPropertyChanged(Job job, String name, Object value) {
		int index = jobs.getFinishedJobs().indexOf(job);
		if (index >= 0)
			fireTableRowsUpdated(index, index);
	}
}
