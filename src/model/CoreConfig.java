package model;

public class CoreConfig {

	private final int coreId;
	private final CoreType coreType;

	public CoreConfig(int coreId, CoreType coreType) {
		this.coreId = coreId;
		this.coreType = coreType;
	}

	public int getCoreId() {
		return coreId;
	}

	public CoreType getCoreType() {
		return coreType;
	}

	public boolean isEnabled() {
		return coreType.isEnabled();
	}
}
