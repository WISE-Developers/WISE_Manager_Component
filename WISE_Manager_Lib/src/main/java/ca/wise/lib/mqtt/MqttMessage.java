package ca.wise.lib.mqtt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;
import ca.wise.lib.json.JobStatus;

/**
 * A message received through MQTT.
 * @author Travis Redpath
 */
public class MqttMessage {
	
	private static ObjectMapper mapper = new ObjectMapper();

	public String timeStampStr;
	
	public LocalDateTime timestamp;
	
	public String from;
	
	public String job;
	
	public String status;
	
	public String topic;
	
	public String message;
	
	public byte[] payload;
	
	public String remoteJobName;
	
	public MessageType type = MessageType.Unknown;
	
	public MqttMessage(String topic, byte[] payload) {
		timestamp = LocalDateTime.now();
		this.topic = topic;
		String[] topicParse = topic.split("/");
		if (topicParse.length > 1) {
			from = topicParse[1];
			if (topicParse.length > 2) {
				job = topicParse[2];
				if (topicParse.length > 3) {
					type = MessageType.fromString(topicParse[3]);
					if (type == MessageType.Status) {
						try {
							JobStatus jStatus = mapper.readValue(payload, JobStatus.class);
							switch (jStatus.status) {
							case 0:
								status = "Submitted";
								break;
							case 1:
								status = "Started";
								break;
							case 2:
								status = "Scenario Started";
								break;
							case 3:
								status = "Scenario Completed";
								break;
							case 4:
								status = "Scenario Failed";
								break;
							case 5:
								status = "Complete";
								break;
							case 6:
								status = "Failed";
								break;
							case 7:
								status = "Error";
								break;
							case 8:
								status = "Information";
								break;
							case 9:
								status = "Shutdown Requested";
								break;
							}
						}
						catch (IOException e) {
							WISELogger.getSpecial(LogName.Backend).warn("Error parsing status payload", e);
							//ignore the status if there was an error
							type = MessageType.Unknown;
						}
					}
					else if ((type == MessageType.JobRemoteStart || type == MessageType.BalanceStart) && topicParse.length > 4) {
						remoteJobName = topicParse[4];
					}
				}
			}
		}
		//the payload for remote job start is a binary file
		if (type == MessageType.JobRemoteStart || type == MessageType.BalanceStart)
			this.payload = payload;
		else
			message = new String(payload, StandardCharsets.UTF_8);
	}
	
	public enum MessageType {
        Unknown,
        Status,
        Shutdown,
        JobRequest,
        JobRemoteRequest,
        JobStart,
        JobRemoteStart,
        Checkin,
        JobHistory,
        Manage,
        FileStream,
        FileResponse,
        /**
         * A new job that is to be load balanced is available to download.
         */
        BalanceRequest,
        /**
         * This instance of manager is available to take a job for load balancing.
         */
        BalanceAvailable,
        /**
         * Store load balanced job file information and start a job or request more file data.
         */
        BalanceStart,
        /**
         * Start a job that was sent over RPC because of load balancing.
         */
        BalanceRpcStart,
        /**
         * List all jobs in the load balance list.
         */
        BalanceList,
        /**
         * A request has been made to list all files that have been output for a specified job.
         */
        ListFiles;
		
		public static MessageType fromString(String value) {
			if (value.equalsIgnoreCase("status"))
				return Status;
			else if (value.equalsIgnoreCase("request"))
				return JobRequest;
			else if (value.equalsIgnoreCase("remoterequest"))
				return JobRemoteRequest;
			else if (value.equalsIgnoreCase("start"))
				return JobStart;
			else if (value.equalsIgnoreCase("remotestart"))
				return JobRemoteStart;
			else if (value.equalsIgnoreCase("shutdown"))
				return Shutdown;
			else if (value.equalsIgnoreCase("reportin"))
				return Checkin;
			else if (value.equalsIgnoreCase("requesthistory"))
				return JobHistory;
			else if (value.equalsIgnoreCase("manage"))
				return Manage;
			else if (value.equalsIgnoreCase("file"))
				return FileStream;
			else if (value.equalsIgnoreCase("fileresponse"))
				return FileResponse;
			else if (value.equalsIgnoreCase("balancerequest"))
			    return BalanceRequest;
			else if (value.equalsIgnoreCase("balanceavailable"))
			    return BalanceAvailable;
			else if (value.equalsIgnoreCase("balancestart"))
			    return BalanceStart;
			else if (value.equalsIgnoreCase("balancerpcstart"))
			    return BalanceRpcStart;
			else if (value.equalsIgnoreCase("balancelist"))
			    return BalanceList;
			else if (value.equalsIgnoreCase("listfiles"))
			    return ListFiles;
			return Unknown;
		}
	}
}
