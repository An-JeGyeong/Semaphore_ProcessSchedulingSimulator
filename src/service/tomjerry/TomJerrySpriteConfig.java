package service.tomjerry;

import model.CoreType;

public final class TomJerrySpriteConfig {

	public static final double MOUSE_WIDTH = 29;
	public static final double MOUSE_HEIGHT = 24;
	public static final double P_CAT_WIDTH = 58;
	public static final double P_CAT_HEIGHT = 56;
	public static final double E_CAT_WIDTH = 52;
	public static final double E_CAT_HEIGHT = 50;

	private static final String P_CAT_IMAGE = "/view/image/cat-p-crop.png";
	private static final String E_CAT_IMAGE = "/view/image/cat-e-crop.png";
	private static final double P_CAT_SPEED = 128;
	private static final double E_CAT_SPEED = 78;
	private static final double P_CAT_CAPTURE_RADIUS = 32;
	private static final double E_CAT_CAPTURE_RADIUS = 27;

	private TomJerrySpriteConfig() {
	}

	// Core 종류에 맞는 고양이 이미지 경로를 반환한다.
	public static String catImagePath(CoreType coreType) {
		return coreType == CoreType.P_CORE ? P_CAT_IMAGE : E_CAT_IMAGE;
	}

	// Core 종류에 따라 고양이 이동 속도를 반환한다.
	public static double catSpeed(CoreType coreType) {
		return coreType == CoreType.P_CORE ? P_CAT_SPEED : E_CAT_SPEED;
	}

	// Core 종류에 따라 생쥐를 잡을 수 있는 거리를 반환한다.
	public static double catCaptureRadius(CoreType coreType) {
		return coreType == CoreType.P_CORE ? P_CAT_CAPTURE_RADIUS : E_CAT_CAPTURE_RADIUS;
	}

	// Core 종류에 맞는 고양이 이미지 너비를 반환한다.
	public static double catWidth(CoreType coreType) {
		return coreType == CoreType.P_CORE ? P_CAT_WIDTH : E_CAT_WIDTH;
	}

	// Core 종류에 맞는 고양이 이미지 높이를 반환한다.
	public static double catHeight(CoreType coreType) {
		return coreType == CoreType.P_CORE ? P_CAT_HEIGHT : E_CAT_HEIGHT;
	}

	// Core 종류와 표시 순서를 고양이 라벨 문구로 변환한다.
	public static String coreLabel(CoreType coreType, int index) {
		return coreType.getDisplayName() + " " + (index + 1);
	}
}
