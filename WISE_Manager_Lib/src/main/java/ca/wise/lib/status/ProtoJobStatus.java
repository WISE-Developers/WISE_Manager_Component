package ca.wise.lib.status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

import ca.wise.status.proto.jobStatus;
import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;

public class ProtoJobStatus implements IJobStatus {
	
	private jobStatus.Builder schema;
	private Path filename;
	private Object mut = new Object();

	public ProtoJobStatus(String directory) {
		filename = Paths.get(directory, "status.json");
		forceReload();
	}
	
	@Override
	public void forceReload() {
		schema = jobStatus.newBuilder();
		if (Files.exists(filename)) {
			File fl = filename.toFile();
			try (BufferedReader stream = new BufferedReader(new FileReader(fl))) {
				JsonFormat.parser().ignoringUnknownFields().merge(stream, schema);
			}
			catch (IOException e) {
				WISELogger.getSpecial(LogName.Backend).fatal("Failed to parse status JSON", e);
			}
		}
	}
	
	@Override
	public void updateStatus(Status newStatus) {
		synchronized(mut) {
			Instant now = Instant.now();
			jobStatus.statusEntry.Builder builder = schema.addEntriesBuilder();
			Timestamp.Builder time = Timestamp.newBuilder()
					.setSeconds(now.getEpochSecond());
			builder.getStatusBuilder()
					.setStatus(statusToPStatus(newStatus))
					.setTime(time);
			saveProto();
		}
	}

	@Override
	public void updateStatus(Status newStatus, String data) {
		synchronized(mut) {
			Instant now = Instant.now();
			jobStatus.statusEntry.Builder builder = schema.addEntriesBuilder();
			Timestamp.Builder time = Timestamp.newBuilder()
					.setSeconds(now.getEpochSecond());
			builder.getStatusBuilder()
					.setStatus(statusToPStatus(newStatus))
					.setTime(time)
					.setData(StringValue.newBuilder().setValue(data));
			saveProto();
		}
	}

	@Override
	public void writeMessage(String message) {
		synchronized(mut) {
			Instant now = Instant.now();
			jobStatus.statusEntry.Builder builder = schema.addEntriesBuilder();
			Timestamp.Builder time = Timestamp.newBuilder()
					.setSeconds(now.getEpochSecond());
			builder.getInformationBuilder()
					.setTime(time)
					.setData(message);
			saveProto();
		}
	}

	@Override
	public Status getCurrentStatus() {
		Status retval = Status.Error;
		synchronized(mut) {
			Optional<Status> opt = schema.getEntriesOrBuilderList().stream()
				.filter(x -> x.getEntryCase() == jobStatus.statusEntry.EntryCase.STATUS)
				.sorted((x, y) -> Long.compare(y.getStatusOrBuilder().getTimeOrBuilder().getSeconds(), x.getStatusOrBuilder().getTimeOrBuilder().getSeconds()))
				.map(x -> pstatusToStatus(x.getStatusOrBuilder().getStatus()))
				.findFirst();
			if (opt.isPresent())
				retval = opt.get();
		}
		return retval;
	}
	
	@Override
	public JobCompletionStatus getJobStatus() {
		JobCompletionStatus retval = JobCompletionStatus.NONE;
		for (int i = 0; i < schema.getEntriesCount(); i++) {
			jobStatus.statusEntryOrBuilder entry = schema.getEntriesOrBuilder(i);
			
			if (entry.getEntryCase() == jobStatus.statusEntry.EntryCase.STATUS) {
				jobStatus.statusUpdateOrBuilder status = entry.getStatusOrBuilder();
				if (status.getStatus() == jobStatus.statusType.COMPLETE)
				{
					retval = JobCompletionStatus.SUCCEEDED;
					break;
				}
				else if (status.getStatus() == jobStatus.statusType.ERROR)
				{
					retval = JobCompletionStatus.FAILED;
					break;
				}
			}
		}
		return retval;
	}
	
	private void saveProto() {
		File fl = filename.toFile();
		try (BufferedWriter stream = new BufferedWriter(new FileWriter(fl))) {
			stream.write(JsonFormat.printer().print(schema));
		}
		catch (IOException e) {
			WISELogger.getSpecial(LogName.Backend).fatal("Failed to save JSON document.", e);
		}
	}
	
	private jobStatus.statusType statusToPStatus(Status status) {
		switch (status) {
		case Submitted:
			return jobStatus.statusType.SUBMITTED;
		case Started:
			return jobStatus.statusType.STARTED;
		case ScenarioStarted:
			return jobStatus.statusType.SCENARIO_STARTED;
		case ScenarioCompleted:
			return jobStatus.statusType.SCENARIO_COMPLETED;
		case ScenarioFailed:
			return jobStatus.statusType.SCENARIO_FAILED;
		case Complete:
			return jobStatus.statusType.COMPLETE;
		case Failed:
			return jobStatus.statusType.FAILED;
		case Error:
			return jobStatus.statusType.ERROR;
		default:
			return jobStatus.statusType.INFORMATION;
		}
	}
	
	private Status pstatusToStatus(jobStatus.statusType status) {
		switch (status) {
		case SUBMITTED:
			return Status.Submitted;
		case STARTED:
			return Status.Started;
		case SCENARIO_STARTED:
			return Status.ScenarioStarted;
		case SCENARIO_COMPLETED:
			return Status.ScenarioCompleted;
		case SCENARIO_FAILED:
			return Status.ScenarioFailed;
		case COMPLETE:
			return Status.Complete;
		case FAILED:
			return Status.Failed;
		case ERROR:
			return Status.Error;
		default:
			return Status.Information;
		}
	}
}
