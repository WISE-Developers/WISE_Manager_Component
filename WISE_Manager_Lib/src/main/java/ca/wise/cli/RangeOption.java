package ca.wise.cli;

import org.apache.commons.cli.Option;

/**
 * A CLI option that only accepts integers between a minimum and maximum value.
 * @author Travis Redpath
 */
public class RangeOption extends Option {
	private static final long serialVersionUID = 1L;
	
	private final int minimum;
	private final int maximum;
	
	/**
	 * Creates an Option using the specified parameters.
	 * @param opt short representation of the option
	 * @param longOpt the long representation of the option
	 * @param hasArg specifies whether the Option takes an argument or not
	 * @param description specifies whether the Option takes an argument or not
	 * @param min the minimum allowed value for the integer parameter
	 * @param max the maximum allowed value for the integer parameter
	 */
	public RangeOption(final String opt, final String longOpt, final boolean hasArg, final String description, final int min, final int max) {
		super(opt, longOpt, hasArg, description);
		minimum = min;
		maximum = max;
	}
	
	/**
	 * Get the integer value specified on the command line.
	 * Throws {@link RuntimeException} if the parameter is not an integer or is not between minimum and maximum.
	 * @return The command line parameter as an integer.
	 */
	public Integer getIValue() {
		final String value = super.getValue();
		if (value == null)
			return null;
		int ivalue = 0;
		try {
			ivalue = Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			invalidValue(value);
		}
		if (ivalue >= minimum && ivalue <= maximum)
			return ivalue;
		invalidValue(value);
		return null;
	}
	
	/**
	 * Get the value specified on the command line.
	 * Throws {@link RuntimeException} if the parameter is not an integer or is not between minimum and maximum.
	 * @return The raw command line parameter.
	 */
	@Override
	public String getValue() {
		return getIValue().toString();
	}
	
	/**
	 * Is the command line parameter valid.
	 * In order to be valid it must be an integer value between minimum and maximum.
	 * @return
	 */
	public boolean isValid() {
		final String value = super.getValue();
		if (value == null)
			return false;
		int ivalue = 0;
		try {
			ivalue = Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return ivalue >= minimum && ivalue <= maximum;
	}
	
	private void invalidValue(String value) {
		throw new RuntimeException(value + " should be an integer value between " + minimum + " and " + maximum);
	}
}
