package ca.wise.lib;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ProcessWatcher implements Runnable {

	protected Consumer<Process> callback;
	protected Process toExecute;
	
	public ProcessWatcher(Consumer<Process> callback, Process toExecute) {
		super();
		this.callback = callback;
		this.toExecute = toExecute;
	}
	
	@Override
	public void run() {
		while (this.toExecute.isAlive()) {
			try {
				this.toExecute.waitFor(250, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				this.toExecute.destroyForcibly();
				return;
			}
		}
		this.callback.accept(this.toExecute);
	}
}
