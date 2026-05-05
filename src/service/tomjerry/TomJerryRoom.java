package service.tomjerry;

import java.util.Random;

import model.TomJerryVector;

public final class TomJerryRoom {

	public static final double WIDTH = 1020;
	public static final double HEIGHT = 760;
	public static final double ROOM_LEFT = 126;
	public static final double ROOM_TOP = 18;
	public static final double ROOM_WIDTH = 767;
	public static final double ROOM_HEIGHT = 720;
	public static final double FALLBACK_SPAWN_X = ROOM_LEFT + 548;
	public static final double FALLBACK_SPAWN_Y = ROOM_TOP + 350;

	private static final double FLOOR_MIN_X = ROOM_LEFT + 82;
	private static final double FLOOR_MAX_X = ROOM_LEFT + 728;
	private static final double FLOOR_MIN_Y = ROOM_TOP + 320;
	private static final double FLOOR_MAX_Y = ROOM_TOP + 684;
	private static final int MAX_RANDOM_TRIES = 200;
	private static final double[][] FLOOR_POLYGON = {
			{ ROOM_LEFT + 82, ROOM_TOP + 520 },
			{ ROOM_LEFT + 295, ROOM_TOP + 390 },
			{ ROOM_LEFT + 520, ROOM_TOP + 315 },
			{ ROOM_LEFT + 710, ROOM_TOP + 410 },
			{ ROOM_LEFT + 728, ROOM_TOP + 560 },
			{ ROOM_LEFT + 545, ROOM_TOP + 684 },
			{ ROOM_LEFT + 240, ROOM_TOP + 662 }
	};

	// 값만 제공하는 유틸리티 클래스라 인스턴스를 만들지 않는다.
	private TomJerryRoom() {
	}

	// 주어진 좌표가 방 이미지의 바닥 다각형 안에 있는지 판정한다.
	// 캐릭터가 벽, 창문, 검은 배경 위로 올라가지 않게 하는 핵심 경계 검사다.
	public static boolean isOnFloor(double x, double y) {
		boolean inside = false;
		for (int i = 0, j = FLOOR_POLYGON.length - 1; i < FLOOR_POLYGON.length; j = i++) {
			double xi = FLOOR_POLYGON[i][0];
			double yi = FLOOR_POLYGON[i][1];
			double xj = FLOOR_POLYGON[j][0];
			double yj = FLOOR_POLYGON[j][1];
			boolean intersects = ((yi > y) != (yj > y))
					&& (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
			if (intersects) {
				inside = !inside;
			}
		}
		return inside;
	}

	// 방 바닥 안에서만 유효한 랜덤 좌표를 반환한다.
	// 랜덤 생성이 실패할 가능성에 대비해 반복 횟수를 제한하고 fallback 좌표를 둔다.
	public static TomJerryVector randomFloorPoint(Random random) {
		for (int i = 0; i < MAX_RANDOM_TRIES; i++) {
			double x = FLOOR_MIN_X + random.nextDouble() * (FLOOR_MAX_X - FLOOR_MIN_X);
			double y = FLOOR_MIN_Y + random.nextDouble() * (FLOOR_MAX_Y - FLOOR_MIN_Y);
			if (isOnFloor(x, y)) {
				return new TomJerryVector(x, y);
			}
		}
		return fallbackSpawnPoint();
	}

	// 랜덤 좌표를 못 찾았을 때 사용할 안전한 바닥 좌표를 반환한다.
	public static TomJerryVector fallbackSpawnPoint() {
		return new TomJerryVector(FALLBACK_SPAWN_X, FALLBACK_SPAWN_Y);
	}

	// X 좌표가 방 바닥 경계 밖으로 나가지 않게 제한한다.
	public static double clampX(double x) {
		return clamp(x, FLOOR_MIN_X, FLOOR_MAX_X);
	}

	// Y 좌표가 방 바닥 경계 밖으로 나가지 않게 제한한다.
	public static double clampY(double y) {
		return clamp(y, FLOOR_MIN_Y, FLOOR_MAX_Y);
	}

	// 값이 지정된 범위를 벗어나지 않도록 제한한다.
	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	// 두 좌표 사이의 직선 거리를 계산한다.
	public static double distance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy);
	}
}
