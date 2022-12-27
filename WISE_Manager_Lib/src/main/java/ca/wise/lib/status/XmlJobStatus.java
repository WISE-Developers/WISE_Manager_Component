package ca.wise.lib.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeFactory;

import ca.wise.lib.WISELogger;
import ca.wise.lib.WISELogger.LogName;
import lombok.SneakyThrows;

public class XmlJobStatus implements IJobStatus {
	
	private ca.wise.lib.xml.List schema;
	private Path filename;
	private Object mut = new Object();
	private JAXBContext jaxbContext;
	
	/**
	 * Create a new status updater.
	 * @param directory The jobs base directory.
	 * @param isUpdater
	 */
	@SneakyThrows
	public XmlJobStatus(String directory) {
		jaxbContext = JAXBContext.newInstance(ca.wise.lib.xml.List.class);
		filename = Paths.get(directory, "status.xml");
		forceReload();
	}
	
	public void forceReload() {
		schema = null;
		if (Files.exists(filename)) {
			try {
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				schema = (ca.wise.lib.xml.List)unmarshaller.unmarshal(filename.toFile());
			}
			catch (JAXBException e) {
				WISELogger.getSpecial(LogName.Backend).fatal("Failed to parse status XML", e);
			}
		}
		if (schema == null)
			schema = new ca.wise.lib.xml.List();
	}
	
	/**
	 * Update the current status of the job.
	 */
	@SneakyThrows
	@Override
	public void updateStatus(Status newStatus) {
		synchronized(mut) {
			ca.wise.lib.xml.List.Status status = new ca.wise.lib.xml.List.Status();
			status.setTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDateTime.now().toString()));
			status.setValue(newStatus.toString());
			schema.getStatusOrMessage().add(status);
			saveXml();
		}
	}
	
	/**
	 * Update the current status of the job.
	 */
	@SneakyThrows
	@Override
	public void updateStatus(Status newStatus, String data) {
		synchronized(mut) {
			ca.wise.lib.xml.List.Status status = new ca.wise.lib.xml.List.Status();
			status.setTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDateTime.now().toString()));
			status.setValue(newStatus.toString());
			status.setData(data);
			schema.getStatusOrMessage().add(status);
			saveXml();
		}
	}
	
	/**
	 * Update the current status of the job.
	 */
	@SneakyThrows
	@Override
	public void writeMessage(String message) {
		synchronized(mut) {
			ca.wise.lib.xml.List.Message status = new ca.wise.lib.xml.List.Message();
			status.setTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDateTime.now().toString()));
			status.setValue(message);
			schema.getStatusOrMessage().add(status);
			saveXml();
		}
	}
	
	protected void saveXml() {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(schema, filename.toFile());
		}
		catch (JAXBException e) {
			WISELogger.getSpecial(LogName.Backend).error("Failed to save XML document.", e);
		}
	}
	
	@SneakyThrows
	@Override
	public Status getCurrentStatus() {
		Status retval = Status.Error;
		synchronized(mut) {
			for (int i = schema.getStatusOrMessage().size() - 1; i >= 0; i--) {
				if (schema.getStatusOrMessage().get(i) instanceof ca.wise.lib.xml.List.Status) {
					retval = statusToStatus((ca.wise.lib.xml.List.Status)schema.getStatusOrMessage().get(i));
					break;
				}
			}
		}
		return retval;
	}
	
	@Override
	public JobCompletionStatus getJobStatus() {
		JobCompletionStatus retval = JobCompletionStatus.NONE;
		for (Object v : schema.getStatusOrMessage()) {
			if (v instanceof ca.wise.lib.xml.List.Status) {
				ca.wise.lib.xml.List.Status status = (ca.wise.lib.xml.List.Status)v;
				if (status.getValue().equals("Complete"))
				{
					retval = JobCompletionStatus.SUCCEEDED;
					break;
				}
				else if (status.getValue().equals("Error"))
				{
					retval = JobCompletionStatus.FAILED;
					break;
				}
			}
		}
		return retval;
	}
	
	private Status statusToStatus(ca.wise.lib.xml.List.Status status) {
		Status retval;
		if (status.getValue().equals("Submitted"))
			retval = Status.Submitted;
        else if (status.getValue().equals("Started"))
        	retval = Status.Started;
        else if (status.getValue().equals("ScenarioStarted"))
        	retval = Status.ScenarioStarted;
        else if (status.getValue().equals("ScenarioCompleted"))
        	retval = Status.ScenarioCompleted;
        else if (status.getValue().equals("ScenarioFailed"))
        	retval = Status.ScenarioFailed;
        else if (status.getValue().equals("Complete"))
        	retval = Status.Complete;
        else if (status.getValue().equals("Failed"))
        	retval = Status.Failed;
        else
        	retval = Status.Error;
		return retval;
	}
}
