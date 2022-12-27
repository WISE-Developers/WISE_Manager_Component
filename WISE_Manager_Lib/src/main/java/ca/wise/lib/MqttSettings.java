package ca.wise.lib;

public class MqttSettings {

	/**
	 * Get the host name of the MQTT broker.
	 */
	public static String getHost() {
		return Settings.getHost();
	}
	
	/**
	 * Set the host name of the MQTT broker.
	 */
	public static void setHost(String host) {
		Settings.setHost(host);
	}
	
	/**
	 * Get the port number to connect to the MQTT broker over.
	 */
	public static int getPort() {
		return Settings.getPort();
	}

	/**
	 * Set the port number to connect to the MQTT broker over.
	 */
	public static void setPort(int port) {
		Settings.setPort(port);
	}
	
	/**
	 * Get the root MQTT topic to communicate using.
	 */
	public static String getTopic() {
		return Settings.getTopic();
	}
	
	/**
	 * Set the root MQTT topic to communicate using.
	 */
	public static void setTopic(String topic) {
		Settings.setTopic(topic);
	}
	
	/**
	 * Get the username used to authenticate on the MQTT broker.
	 */
	public static String getUser() {
		return Settings.getUser();
	}

	/**
	 * Set the username used to authenticate on the MQTT broker.
	 */
	public static void setUser(String user) {
		Settings.setUser(user);
	}
	
	/**
	 * Get the password used to authenticate on the MQTT broker.
	 */
	//TODO this should be encrypted somehow
	public static String getPassword() {
		return Settings.getPassword();
	}

	/**
	 * Set the password used to authenticate on the MQTT broker.
	 */
	public static void setPassword(String pass) {
		Settings.setPassword(pass);
	}
	
	/**
	 * Get the MQTT ID to use. If null or empty generate an ID automatically.
	 */
	public static String getMqttId() {
	    return Settings.getMqttId();
	}
	
	/**
	 * Set the MQTT ID that should be used.
	 */
	public static void setMqttId(String value) {
	    Settings.setMqttId(value);
	}
	
	/**
	 * Get the setting for whether the manager's internal broker should
	 * be started and connected to instead of an external broker.
	 */
	public static boolean useInternalBroker() {
		return Settings.useInternalBroker();
	}
	
	/**
	 * Set whether manager's internal broker should be started and used.
	 * @param value True to use manager's internal broker, false if an
	 * external broker will be used.
	 */
	public static void setUseInternalBroker(boolean value) {
		Settings.setUseInternalBroker(value);
	}
	
	/**
	 * Should the provided authentication be used.
	 * @return If false any stored authentication information should be
	 * ignored when connecting to the MQTT broker.
	 */
	public static boolean useAuthentication() {
		return Settings.useAuthentication();
	}
	
	/**
	 * Set whether the provided authentication information should be used.
	 * @param value True to use the authentication information, false to ignore it.
	 */
	public static void setUseAuthentication(boolean value) {
		Settings.setUseAuthentication(value);
	}
	
	public static void snapshot() {
	    Settings.snapshot();
	}
}
