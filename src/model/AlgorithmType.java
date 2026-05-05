package model;

public enum AlgorithmType {
	FCFS, RR, SPN, SRTN, HRRN, CUSTOM;

	@Override
	public String toString() {
		return this == CUSTOM ? "Tom & Jerry" : name();
	}
}
