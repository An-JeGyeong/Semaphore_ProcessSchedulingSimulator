package service;

import java.util.List;

import model.AlgorithmType;
import model.CoreConfig;
import model.Process;

final class SchedulingValidator {

	private SchedulingValidator() {
	}

	static void validate(AlgorithmType algorithm, List<Process> processes, List<CoreConfig> cores, int timeQuantum) {
		if (algorithm == null) {
			throw new IllegalArgumentException("알고리즘을 선택하세요.");
		}
		if (processes == null || processes.isEmpty()) {
			throw new IllegalArgumentException("프로세스를 먼저 추가하세요.");
		}
		if (cores == null || cores.isEmpty()) {
			throw new IllegalArgumentException("하나 이상의 Core를 선택하세요.");
		}
		if (algorithm == AlgorithmType.RR && timeQuantum <= 0) {
			throw new IllegalArgumentException("Time Quantum은 1 이상이어야 합니다.");
		}
	}
}
