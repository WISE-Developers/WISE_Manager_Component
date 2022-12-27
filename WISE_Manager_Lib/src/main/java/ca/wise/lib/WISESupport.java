package ca.wise.lib;

import lombok.Getter;
import lombok.Setter;

/**
 * What options are supported by the selected version of W.I.S.E..
 * @author Travis Redpath
 */
public class WISESupport {

	/**
	 * Does the selected version of W.I.S.E. accept the manager ID
	 * as a command line argument.
	 */
	@Getter @Setter private static boolean supportsManagerId;
	
	/**
	 * The manager ID to use as a command line argument if
	 * W.I.S.E. supports it.
	 */
	@Getter @Setter private static String mqttId;
	
	/**
	 * Does the selected version of W.I.S.E. support validating
	 * jobs.
	 */
	@Getter @Setter private static boolean supportsValidation;
}
