package ca.wise.lib.status;

/**
 * The different status that the job can have.
 * @author Travis Redpath
 */
public enum Status {
	/**
	 * The job was submitted to the manager.
	 */
	Submitted,
	/**
	 * A new job has been started. This will be the current status when Prometheus starts up.
	 */
	Started,
	/**
	 * A new scenario has started.
	 */
	ScenarioStarted,
	/**
	 * A scenario has finished.
	 */
	ScenarioCompleted,
	/**
	 * A scenario failed to finish properly.
	 */
	ScenarioFailed,
	/**
	 * The job has completed successfully.
	 */
	Complete,
	/**
	 * Prometheus ended in an unexpected manner. This shouldn't be used by Prometheus,
	 * the manager will mark the job as Failed if the process
     * is terminated without being marked as either Complete or Error.
	 */
	Failed,
	/**
	 * An exception was caught within Prometheus and the job was terminated.
	 */
	Error,
	/**
	 * An informational note.
	 */
	Information
}
