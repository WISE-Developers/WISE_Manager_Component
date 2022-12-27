package ca.wise.lib.json;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class JobHistory {

	@JsonProperty("job_name")
	public String name;
	
	@JsonProperty("status")
	public HistoryStatus status;
	
	@JsonProperty("submit_time")
	public String submitTime;
	
	@JsonProperty("start_time")
	public String startTime;
	
	@JsonProperty("complete_time")
	public String completeTime;
	
	public enum HistoryStatus {
		Unknown("unknown"),
		All("all"),
		Queued("queue"),
		Running("run"),
		QueueRun("queuerun"),
		Complete("complete");
		
		private final String value;
		
		HistoryStatus(String value) {
			this.value = value;
		}
		
		@JsonCreator
		public static HistoryStatus forValue(final String value) {
			return Arrays.stream(values())
					.filter(x -> x.value.equals(value))
					.findFirst()
					.orElse(HistoryStatus.Unknown);
		}
		
		@JsonValue
		public String toValue() {
			return value;
		}
	}
}
