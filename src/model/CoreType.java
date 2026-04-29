package model;

//enum: 코어 타입을 정의 (OFF / E_CORE / P_CORE)
public enum CoreType {

	OFF(0, 0.0, 0.0),
	E_CORE(1, 1.0, 0.1), // 효율 코어 (1초에 1 처리, 1W, 시동 0.1W)
	P_CORE(2, 3.0, 0.5); // 성능 코어 (1초에 2 처리, 3W, 시동 0.5W)

	// 코어의 스펙 정보 저장 변수
	private final int performancePerSecond; // 1초에 처리 가능한 작업량
	private final double powerPerSecond; // 1초 동안 소비하는 전력 (W)
	private final double startupPower; // idle → 실행 시 추가 전력

	// 생성자 (enum 초기화 시 자동 호출)
	CoreType(int performancePerSecond, double powerPerSecond, double startupPower) {
		this.performancePerSecond = performancePerSecond;
		this.powerPerSecond = powerPerSecond;
		this.startupPower = startupPower;
	}

	// 1초당 처리량 반환
	public int getPerformancePerSecond() {
		return performancePerSecond;
	}

	// 1초당 소비 전력 반환
	public double getPowerPerSecond() {
		return powerPerSecond;
	}

	// 코어 시작 시 소비되는 전력 반환
	public double getStartupPower() {
		return startupPower;
	}

	// 코어 사용 여부 판단
	// OFF면 false, 나머지는 true
	public boolean isEnabled() {
		return this != OFF;
	}
}
