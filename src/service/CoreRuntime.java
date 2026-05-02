package service;

import model.CoreConfig;

final class CoreRuntime {

	final int index;
	final CoreConfig config;
	ProcessState current;
	int quantumUsed;

	CoreRuntime(int index, CoreConfig config) {
		this.index = index;
		this.config = config;
	}

	void assign(ProcessState state) {
		current = state;
		quantumUsed = 0;
	}

	void clear() {
		current = null;
		quantumUsed = 0;
	}
}
