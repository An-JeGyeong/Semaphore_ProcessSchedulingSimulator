package model;

public enum CoreType {

	OFF(0, 0.0, 0.0),
	E_CORE(1, 1.0, 0.1),
	P_CORE(2, 3.0, 0.5);

	private final int performancePerSecond;
	private final double powerPerSecond;
	private final double startupPower;

	CoreType(int performancePerSecond, double powerPerSecond, double startupPower) {
		this.performancePerSecond = performancePerSecond;
		this.powerPerSecond = powerPerSecond;
		this.startupPower = startupPower;
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
}
