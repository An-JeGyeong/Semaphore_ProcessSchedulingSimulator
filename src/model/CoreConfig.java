package model;

//각 코어의 설정 정보를 저장하는 클래스
public class CoreConfig {

	// 코어 번호
	private final int coreId;

	// 코어 타입
	private final CoreType coreType;

	// 코어 번호와 코어 타입을 받아서 저장
	public CoreConfig(int coreId, CoreType coreType) {
		this.coreId = coreId;
		this.coreType = coreType;
	}

	// 코어 번호 반환
	public int getCoreId() {
		return coreId;
	}

	// 코어 타입 반환
	public CoreType getCoreType() {
		return coreType;
	}

	// 코어가 사용 가능한 상태인지 확인
	public boolean isEnabled() {
		return coreType.isEnabled();
	}
}