package controller;

final class ProcessColorPalette {

	private static final String DEFAULT_COLOR = "#A6A6A6";
	private static final String[] PROCESS_COLORS = {
			"#E0531F", "#1FA2E0", "#82DF5A", "#C77DFF", "#FFD166",
			"#FF6B6B", "#F26A3D", "#4DB8F2", "#A8F080", "#D9A3FF",
			"#FFE08A", "#FF9A9A", "#A63A14", "#1479A6", "#4F9F2F"
	};

	private ProcessColorPalette() {
	}

	static String getColor(String pid) {
		int processNumber = parseProcessNumber(pid);
		if (processNumber <= 0) {
			return DEFAULT_COLOR;
		}

		return PROCESS_COLORS[(processNumber - 1) % PROCESS_COLORS.length];
	}

	static int parseProcessNumber(String pid) {
		if (pid == null || pid.length() < 2 || pid.charAt(0) != 'P') {
			return -1;
		}

		try {
			return Integer.parseInt(pid.substring(1));
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
