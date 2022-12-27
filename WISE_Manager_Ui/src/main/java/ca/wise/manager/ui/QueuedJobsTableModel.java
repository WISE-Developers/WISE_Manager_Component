package ca.wise.manager.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.hss.tr.Resources;
import ca.hss.tr.Translations;
import ca.wise.lib.Job;
import ca.wise.lib.JobLists;
import ca.wise.lib.Job.IPropertyChangedListener;
import ca.wise.lib.Job.JobStage;
import ca.wise.lib.JobLists.IListChangedListener;

public class QueuedJobsTableModel extends AbstractTableModel implements IListChangedListener, IPropertyChangedListener {
	private static final long serialVersionUID = 1L;
	
	private JobLists jobs;
	private Resources resources;

	public QueuedJobsTableModel(JobLists jobs) {
		this.jobs = jobs;
		this.jobs.addQueueChangeListener(this);
		resources = Translations.getNamedResources("app");
	}
	
	/**
	 * Get the ability of a job to be moved up or down. A job cannot be moved
	 * up if it is at the top of the list or the only jobs above it are already
	 * running. A job cannot be moved down if it is at the end of the list.
	 * @param index The index of the job to check.
	 */
	public MoveStatus getJobMoveStatus(int index) {
		List<Job> data = jobs.lockJobQueue();
		boolean up = false;
		boolean down = false;
		try {
			if (index >= 0 && index < data.size()) {
				Job job = data.get(index);
				if (job.getStatus() != JobStage.Running) {
					if (index > 0) {
						if (data.get(index - 1).getStatus() != JobStage.Running)
							up = true;
					}
					if (index < (data.size() - 1))
						down = true;
				}
			}
		}
		finally {
			jobs.unlockJobQueue();
		}
		if (up && down)
			return MoveStatus.UP_DOWN;
		else if (up)
			return MoveStatus.UP;
		else if (down)
			return MoveStatus.DOWN;
		return MoveStatus.NONE;
	}
	
	/**
	 * Get the name of the job at the given index.
	 * @param index The index of the job in the list.
	 * @return The name (ID) of the job, or null if no job exists at the requested index.
	 */
	public String getJobId(int index) {
		String id = null;
		List<Job> data = jobs.lockJobQueue();
		try {
			if (index >= 0 && index < data.size()) {
				id = data.get(index).getName();
			}
		}
		finally {
			jobs.unlockJobQueue();
		}
		return id;
	}
	
	/**
	 * Is the job at the given index currently running.
	 * @param index The index of the job in the list.
	 * @return True if the job is running, false if it is queued.
	 */
	public boolean isRunning(int index) {
		boolean retval = false;
		List<Job> data = jobs.lockJobQueue();
		try {
			if (index >= 0 && index < data.size()) {
				retval = data.get(index).getStatus() == JobStage.Running;
			}
		}
		finally {
			jobs.unlockJobQueue();
		}
		return retval;
	}
	
	/**
	 * If possible move a job up the job queue.
	 * @param index The index of the job to move.
	 */
	public boolean moveJobUp(int index) {
		if (jobs.moveJobUp(index)) {
			fireTableRowsUpdated(index - 1, index);
			return true;
		}
		return false;
	}
	
	/**
	 * If possible move a job down the job queue.
	 * @param index The index of the job to move.
	 */
	public boolean moveJobDown(int index) {
		if (jobs.moveJobDown(index)) {
			fireTableRowsUpdated(index, index + 1);
			return true;
		}
		return false;
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public int getRowCount() {
		return jobs.getJobQueue().size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		List<Job> data = jobs.lockJobQueue();
		try {
			if (row < data.size()) {
				switch (col) {
				case 0:
					return data.get(row).getName();
				case 1:
					return data.get(row).getSubmitted();
				case 2:
					return data.get(row).getStart();
				case 3:
					return data.get(row).getRequestedCores();
				case 4:
					return String.format("%.2f", data.get(row).getUsage());
				}
			}
		}
		finally {
			jobs.unlockJobQueue();
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
			return resources.getString("job.submitted");
		case 2:
			return resources.getString("job.start");
		case 3:
			return resources.getString("job.cores");
		case 4:
			return resources.getString("job.usage");
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
		int index = jobs.getJobQueue().indexOf(job);
		if (index >= 0)
			fireTableRowsUpdated(index, index);
	}
	
	public static enum MoveStatus {
		NONE,
		UP,
		DOWN,
		UP_DOWN
	}
}
