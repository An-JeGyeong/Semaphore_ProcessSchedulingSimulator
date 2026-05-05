package service.tomjerry;

import java.util.Random;

import model.TomJerryVector;

public final class TomJerryPhysicsService {

	private static final double MIN_MOUSE_SPEED = 44;
	private static final double MAX_MOUSE_SPEED = 86;
	private static final double REDIRECT_MOUSE_SPEED = 58;
	private static final double REDIRECT_MOUSE_SPEED_RANGE = 35;
	private static final double CAT_AVOID_DISTANCE = 112;
	private static final double CAT_AVOID_SPEED = 105;
	private static final double BOUNCE_DECAY = -0.75;

	private TomJerryPhysicsService() {
	}

	// 랜덤 각도와 속도로 생쥐의 기본 이동 벡터를 만든다.
	public static TomJerryVector randomMouseVelocity(Random random) {
		double angle = random.nextDouble() * Math.PI * 2;
		double speed = MIN_MOUSE_SPEED + random.nextDouble() * (MAX_MOUSE_SPEED - MIN_MOUSE_SPEED);
		return new TomJerryVector(Math.cos(angle) * speed, Math.sin(angle) * speed);
	}

	// 고양이에게 너무 가까워진 생쥐가 반대 방향으로 도망갈 속도를 계산한다.
	public static TomJerryVector escapeVelocity(double mouseX, double mouseY, double catX, double catY) {
		double dx = mouseX - catX;
		double dy = mouseY - catY;
		double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
		return new TomJerryVector(dx / length * CAT_AVOID_SPEED, dy / length * CAT_AVOID_SPEED);
	}

	// 고양이가 회피 반응을 일으킬 만큼 가까운지 확인한다.
	public static boolean shouldAvoidCat(double distance) {
		return distance <= CAT_AVOID_DISTANCE;
	}

	// 목표 좌표를 향해 한 프레임 동안 이동할 다음 위치를 계산한다.
	public static TomJerryVector moveToward(double x, double y, double targetX, double targetY, double speed, double delta) {
		double dx = targetX - x;
		double dy = targetY - y;
		double length = Math.sqrt(dx * dx + dy * dy);
		if (length < 1) {
			return new TomJerryVector(x, y);
		}

		double step = Math.min(speed * delta, length);
		return new TomJerryVector(
				TomJerryRoom.clampX(x + dx / length * step),
				TomJerryRoom.clampY(y + dy / length * step));
	}

	// 현재 속도를 적용한 다음 생쥐 위치를 계산한다.
	public static TomJerryVector moveByVelocity(double x, double y, double vx, double vy, double delta) {
		return new TomJerryVector(
				TomJerryRoom.clampX(x + vx * delta),
				TomJerryRoom.clampY(y + vy * delta));
	}

	// 바닥 경계에 부딪힌 생쥐가 튕겨 나오는 속도를 계산한다.
	public static TomJerryVector bouncedVelocity(double vx, double vy) {
		return new TomJerryVector(vx * BOUNCE_DECAY, vy * BOUNCE_DECAY);
	}

	// 막힌 위치에서 새로운 바닥 좌표를 향하도록 생쥐 속도를 다시 만든다.
	public static TomJerryVector redirectMouseVelocity(double x, double y, TomJerryVector target, Random random) {
		return normalize(
				target.x() - x,
				target.y() - y,
				REDIRECT_MOUSE_SPEED + random.nextDouble() * REDIRECT_MOUSE_SPEED_RANGE);
	}

	// 주어진 벡터를 원하는 속도 크기로 정규화한다.
	public static TomJerryVector normalize(double vx, double vy, double speed) {
		double length = Math.max(1, Math.sqrt(vx * vx + vy * vy));
		return new TomJerryVector(vx / length * speed, vy / length * speed);
	}
}
