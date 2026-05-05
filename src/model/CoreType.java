package model;

public enum CoreType {

	// 전력/성능 조건은 과제 명세의 P-Core, E-Core 기준을 그대로 모델에 둔다.
	OFF("off", 0, 0.0, 0.0),
	E_CORE("E-Core", 1, 1.0, 0.1),
	P_CORE("P-Core", 2, 3.0, 0.5);

	private final String displayName;
	private final int performancePerSecond;
	private final double powerPerSecond;
	private final double startupPower;

	CoreType(String displayName, int performancePerSecond, double powerPerSecond, double startupPower) {
		this.displayName = displayName;
		this.performancePerSecond = performancePerSecond;
		this.powerPerSecond = powerPerSecond;
		this.startupPower = startupPower;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getPerformancePerSecond() {
		return performancePerSecond;
	}

	public double getPowerPerSecond() {
		return powerPerSecond;
	}

	public double getStartupPower() {
		return startupPower;
	}

	public boolean isEnabled() {
		return this != OFF;
	}

	// 라디오 버튼이나 Overview에 표시되는 문자열을 CoreType으로 되돌린다.
	public static CoreType fromDisplayName(String displayName) {
		for (CoreType coreType : values()) {
			if (coreType.displayName.equalsIgnoreCase(displayName)) {
				return coreType;
			}
		}
		return OFF;
	}
}
