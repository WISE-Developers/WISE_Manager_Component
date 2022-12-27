package com.g00fy2.versioncompare;

import java.util.List;
import java.util.regex.Pattern;

public class VersionComparator {

	// position of SemVer version part
	static final int MAJOR = 0;
	static final int MINOR = 1;
	static final int PATCH = 2;

	// supported PreRelease suffixes
	private static final String PRE_STRING = "pre";
	private static final String ALPHA_STRING = "alpha";
	private static final String BETA_STRING = "beta";
	private static final String RC_STRING = "rc";

	// weighting of the PreRelease suffixes
	private static final int PRE_ALPHA = 0;
	private static final int ALPHA = 1;
	private static final int BETA = 2;
	private static final int RC = 3;
	private static final int UNKNOWN = 4;

	// regex to find numeric characters
	static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");

	static int compareSubversionNumbers(final List<Integer> subversionsA,
			final List<Integer> subversionsB, boolean limitCompare) {
		final int versASize = subversionsA.size();
		final int versBSize = subversionsB.size();
		int maxSize = limitCompare ? versBSize : Math.max(versASize, versBSize);
		
		for (int i = 0; i < maxSize; i++) {
			if ((i < versASize ? subversionsA.get(i) : 0) > (i < versBSize ? subversionsB.get(i) : 0)) {
				return 1;
			} else if ((i < versASize ? subversionsA.get(i) : 0) < (i < versBSize ? subversionsB.get(i) : 0)) {
				return -1;
			}
		}
		return 0;
}

	static int compareSuffix(final String suffixA, final String suffixB) {
		if (suffixA.length() > 0 || suffixB.length() > 0) {
			int qualifierA = qualifierToNumber(suffixA);
			int qualifierB = qualifierToNumber(suffixB);

			if (qualifierA > qualifierB) {
				return 1;
			} else if (qualifierA < qualifierB) {
				return -1;
			} else if (qualifierA != UNKNOWN && qualifierB != UNKNOWN) {
				int suffixVersionA = preReleaseVersion(suffixA, qualifierA);
				int suffixVersionB = preReleaseVersion(suffixB, qualifierB);
				
				if (suffixVersionA > suffixVersionB) {
					return 1;
				} else if (suffixVersionA < suffixVersionB) {
					return -1;
				}
			}
		}
		return 0;
	}

	private static int qualifierToNumber(String suffix) {
		if (suffix.length() > 0) {
			suffix = suffix.toLowerCase();
			if (suffix.contains(RC_STRING)) return RC;
			if (suffix.contains(BETA_STRING)) return BETA;
			if (suffix.contains(ALPHA_STRING)) {
				if (suffix.contains(PRE_STRING)) {
					return PRE_ALPHA;
				} else {
					return ALPHA;
				}
			}
		}
		return UNKNOWN;
	}

	private static int preReleaseVersion(String suffix, int qualifier) {
		int startIndex = indexOfQualifier(suffix, qualifier);
		if (startIndex < suffix.length()) {
			int maxStartIndex = Math.min(startIndex + 2, suffix.length());
			if (NUMERIC_PATTERN.matcher(suffix.substring(startIndex, maxStartIndex)).find()) {
				StringBuilder versionNumber = new StringBuilder();
				for (int i = startIndex, numIndex = -1; i < suffix.length() && (numIndex == -1 || numIndex + 1 == i); i++) {
					if (NUMERIC_PATTERN.matcher(String.valueOf(suffix.charAt(i))).matches()) {
						numIndex = i;
						versionNumber.append(suffix.charAt(i));
					}
				}
				return safeParseInt(versionNumber.toString());
			}
		}
		return 0;
	}

	private static int indexOfQualifier(String suffix, int qualifier) {
		if (qualifier == RC) return suffix.indexOf(RC_STRING) + RC_STRING.length();
		if (qualifier == BETA) return suffix.indexOf(BETA_STRING) + BETA_STRING.length();
		if (qualifier == ALPHA || qualifier == PRE_ALPHA) return suffix.indexOf(ALPHA_STRING) + ALPHA_STRING.length();
		return 0;
	}

	// helper methods
	static boolean startsNumeric(String str) {
		str = str.trim();
		return str.length() > 0 && NUMERIC_PATTERN.matcher(String.valueOf(str.charAt(0))).matches();
	}

	static int safeParseInt(String numbers) {
		if (numbers.length() > 9) {
			numbers = numbers.substring(0, 9);
		}
		return Integer.parseInt(numbers);
	}
}
