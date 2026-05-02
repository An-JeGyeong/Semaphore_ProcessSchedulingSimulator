package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.CoreConfig;
import model.CoreType;
import model.Process;
import model.SchedulingResult;
import service.tomjerry.TomJerrySchedulingService;

final class TomJerrySimulationWindow {

	private static final double WIDTH = 1020;
	private static final double HEIGHT = 760;
	private static final double ROOM_LEFT = 126;
	private static final double ROOM_TOP = 18;
	private static final double FLOOR_MIN_X = ROOM_LEFT + 82;
	private static final double FLOOR_MAX_X = ROOM_LEFT + 728;
	private static final double FLOOR_MIN_Y = ROOM_TOP + 320;
	private static final double FLOOR_MAX_Y = ROOM_TOP + 684;
	private static final double FALLBACK_SPAWN_X = ROOM_LEFT + 548;
	private static final double FALLBACK_SPAWN_Y = ROOM_TOP + 350;
	private static final double SIM_SPEED = 1.35;
	private static final double[][] FLOOR_POLYGON = {
			{ ROOM_LEFT + 82, ROOM_TOP + 520 },
			{ ROOM_LEFT + 295, ROOM_TOP + 390 },
			{ ROOM_LEFT + 520, ROOM_TOP + 315 },
			{ ROOM_LEFT + 710, ROOM_TOP + 410 },
			{ ROOM_LEFT + 728, ROOM_TOP + 560 },
			{ ROOM_LEFT + 545, ROOM_TOP + 684 },
			{ ROOM_LEFT + 240, ROOM_TOP + 662 }
	};

	private final Random random = new Random();
	private final Pane world = new Pane();
	private final Label timeLabel = new Label("Time 0");
	private final List<MouseAgent> mice = new ArrayList<>();
	private final List<CatAgent> cats = new ArrayList<>();
	private final DialogController dialogController = new DialogController();
	private final TomJerrySchedulingService schedulingService = new TomJerrySchedulingService();

	private AnimationTimer timer;
	private Listener listener;
	private Stage stage;
	private double elapsedSimTime;
	private long lastFrame;
	private int previousTick = -1;
	private boolean completionShown;

	void show(List<Process> processes, List<CoreConfig> cores, Listener listener) {
		closePreviousStage();
		this.listener = listener;
		stage = new Stage();
		stage.setTitle("Tom & Jerry Scheduling");
		stage.setResizable(true);

		StackPane root = new StackPane(world);
		root.setStyle("-fx-background-color: #101010;");
		Scene scene = new Scene(root, WIDTH, HEIGHT, Color.web("#101010"));
		stage.setScene(scene);

		setupWorld();
		setupProcesses(processes);
		setupCores(cores);
		startLoop(stage);

		stage.setOnCloseRequest(event -> {
			stopLoop();
			stage = null;
		});
		stage.show();
	}

	private void closePreviousStage() {
		stopLoop();
		if (stage != null) {
			stage.close();
			stage = null;
		}
	}

	private void setupWorld() {
		world.getChildren().clear();
		world.setPrefSize(WIDTH, HEIGHT);
		world.setStyle("-fx-background-color: #101010;");

		ImageView room = createImage("/view/image/tomjerry/room.png", 767, 720);
		room.setLayoutX(ROOM_LEFT);
		room.setLayoutY(ROOM_TOP);
		world.getChildren().add(room);

		Label title = new Label("Tom & Jerry Scheduling");
		title.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 20px; -fx-text-fill: #f2f2f2; -fx-font-weight: bold;");
		title.setLayoutX(24);
		title.setLayoutY(18);
		world.getChildren().add(title);

		timeLabel.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 13px; -fx-text-fill: #bdbdbd;");
		timeLabel.setLayoutX(24);
		timeLabel.setLayoutY(48);
		world.getChildren().add(timeLabel);
	}

	private void setupProcesses(List<Process> processes) {
		mice.clear();
		schedulingService.reset();
		List<Process> sorted = new ArrayList<>(processes);
		sorted.sort(java.util.Comparator
				.comparingInt(Process::getArrivalTime)
				.thenComparing(Process::getPid));

		for (Process process : sorted) {
			mice.add(new MouseAgent(process));
		}
	}

	private void setupCores(List<CoreConfig> cores) {
		cats.clear();
		int visibleIndex = 0;

		for (CoreConfig core : cores) {
			if (!core.isEnabled()) {
				continue;
			}

			double[] point = randomFloorPoint();
			double x = point[0];
			double y = point[1];
			CatAgent cat = new CatAgent(core, visibleIndex, x, y);
			cats.add(cat);
			world.getChildren().addAll(cat.view, cat.label);
			cat.render();
			visibleIndex++;
		}
	}

	private void startLoop(Stage stage) {
		stopLoop();
		elapsedSimTime = 0;
		lastFrame = 0;
		previousTick = -1;
		completionShown = false;

		timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				if (lastFrame == 0) {
					lastFrame = now;
					return;
				}

				double delta = Math.min(0.05, (now - lastFrame) / 1_000_000_000.0);
				lastFrame = now;
				elapsedSimTime += delta * SIM_SPEED;
				update(delta * SIM_SPEED);

				if (allMiceCompleted()) {
					stopLoop();
					showCompletionDialog();
				}
			}
		};
		timer.start();

		stage.focusedProperty().addListener((observable, oldValue, focused) -> {
			if (focused) {
				lastFrame = 0;
			}
		});
	}

	private void stopLoop() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
	}

	private void update(double delta) {
		int tick = (int) Math.floor(elapsedSimTime);
		if (tick != previousTick) {
			previousTick = tick;
			timeLabel.setText("Time " + tick);
			spawnArrivals(tick);
			updateBusyCats(tick);
		}

		for (MouseAgent mouse : mice) {
			if (mouse.active && !mouse.completed) {
				mouse.update(delta);
			}
		}

		for (CatAgent cat : cats) {
			cat.update(delta);
		}
	}

	private void spawnArrivals(int tick) {
		for (MouseAgent mouse : mice) {
			if (!mouse.spawned && mouse.process.getArrivalTime() <= tick) {
				mouse.spawn();
				world.getChildren().addAll(mouse.view, mouse.label);
				mouse.render();
			}
		}
	}

	private MouseAgent chooseTargetFor(CatAgent cat) {
		return schedulingService.chooseTarget(cat.target, mice, random);
	}

	private void updateBusyCats(int tick) {
		boolean changed = false;
		for (CatAgent cat : cats) {
			changed |= cat.updateExecution(tick);
		}
		if (changed) {
			publishResult();
		}
	}

	private boolean allMiceCompleted() {
		return schedulingService.allCompleted(mice);
	}

	private void showCompletionDialog() {
		if (completionShown) {
			return;
		}

		completionShown = true;
		Platform.runLater(() -> dialogController.showWarning(
				"Simulation Complete",
				"Tom & Jerry Scheduling이 완료되었습니다."));
	}

	private ImageView createImage(String path, double fitWidth, double fitHeight) {
		ImageView view = new ImageView(new Image(getClass().getResource(path).toExternalForm()));
		view.setFitWidth(fitWidth);
		view.setFitHeight(fitHeight);
		view.setPreserveRatio(true);
		view.setSmooth(false);
		return view;
	}

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private boolean isOnFloor(double x, double y) {
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

	private double[] randomFloorPoint() {
		for (int i = 0; i < 200; i++) {
			double x = FLOOR_MIN_X + random.nextDouble() * (FLOOR_MAX_X - FLOOR_MIN_X);
			double y = FLOOR_MIN_Y + random.nextDouble() * (FLOOR_MAX_Y - FLOOR_MIN_Y);
			if (isOnFloor(x, y)) {
				return new double[] { x, y };
			}
		}
		return new double[] { FALLBACK_SPAWN_X, FALLBACK_SPAWN_Y };
	}

	private double distance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private void publishResult() {
		if (listener != null) {
			listener.onSimulationUpdated(schedulingService.getResult());
		}
	}

	private final class CatAgent {
		private final CoreConfig core;
		private final ImageView view;
		private final Label label;
		private final double speed;
		private final double captureRadius;
		private final int coreIndex;
		private double x;
		private double y;
		private double wanderX;
		private double wanderY;
		private double decisionCooldown;
		private int facing = 1;
		private MouseAgent target;
		private MouseAgent executingMouse;
		private final TomJerrySchedulingService.CoreExecution execution = new TomJerrySchedulingService.CoreExecution();
		private Mode mode = Mode.WANDER;

		private CatAgent(CoreConfig core, int index, double x, double y) {
			this.core = core;
			this.coreIndex = index;
			this.x = x;
			this.y = y;
			this.wanderX = x;
			this.wanderY = y;
			this.speed = core.getCoreType() == CoreType.P_CORE ? 128 : 78;
			this.captureRadius = core.getCoreType() == CoreType.P_CORE ? 32 : 27;
			this.view = createImage(
					core.getCoreType() == CoreType.P_CORE
							? "/view/image/tomjerry/cat-p-crop.png"
							: "/view/image/tomjerry/cat-e-crop.png",
					core.getCoreType() == CoreType.P_CORE ? 58 : 52,
					core.getCoreType() == CoreType.P_CORE ? 56 : 50);
			this.label = new Label(coreLabel(core, index));
			this.label.setAlignment(Pos.CENTER);
			this.label.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 11px; -fx-text-fill: #f2f2f2; -fx-background-color: rgba(20,20,20,0.6); -fx-background-radius: 4; -fx-padding: 2 6;");
		}

		private void update(double delta) {
			if (executingMouse != null) {
				render();
				return;
			}

			decisionCooldown -= delta;
			if (decisionCooldown <= 0) {
				decideNextMove();
			}

			double targetX = mode == Mode.CHASE && target != null ? target.x : wanderX;
			double targetY = mode == Mode.CHASE && target != null ? target.y : wanderY;
			moveToward(targetX, targetY, delta);
			tryCapture();
			render();
		}

		private void decideNextMove() {
			decisionCooldown = 0.35 + random.nextDouble() * 0.65;

			if (target == null || target.completed || !target.active || random.nextDouble() < 0.42) {
				target = chooseTargetFor(this);
			}

			double roll = random.nextDouble();
			if (target != null && roll < 0.78) {
				mode = Mode.CHASE;
			} else if (roll < 0.94) {
				mode = Mode.WANDER;
				double[] point = randomFloorPoint();
				wanderX = point[0];
				wanderY = point[1];
			} else {
				mode = Mode.IDLE;
				wanderX = x;
				wanderY = y;
			}
		}

		private void moveToward(double targetX, double targetY, double delta) {
			if (mode == Mode.IDLE) {
				return;
			}

			double dx = targetX - x;
			double dy = targetY - y;
			double length = Math.sqrt(dx * dx + dy * dy);
			if (length < 1) {
				return;
			}

			double step = Math.min(speed * delta, length);
			double nextX = clamp(x + dx / length * step, FLOOR_MIN_X, FLOOR_MAX_X);
			double nextY = clamp(y + dy / length * step, FLOOR_MIN_Y, FLOOR_MAX_Y);
			if (isOnFloor(nextX, nextY)) {
				updateFacing(nextX - x);
				x = nextX;
				y = nextY;
			} else {
				double[] point = randomFloorPoint();
				wanderX = point[0];
				wanderY = point[1];
				mode = Mode.WANDER;
			}
		}

		private void tryCapture() {
			if (target == null || target.completed || !target.active) {
				return;
			}
			if (distance(x, y, target.x, target.y) > captureRadius) {
				return;
			}

			startExecution(target);
			target = null;
		}

		private void startExecution(MouseAgent mouse) {
			mouse.caught = true;
			mouse.hideImmediately();
			executingMouse = mouse;
			mode = Mode.IDLE;
			if (schedulingService.startExecution(execution, mouse.process, core, coreIndex, previousTick)) {
				publishResult();
			}
		}

		private boolean updateExecution(int tick) {
			if (executingMouse == null) {
				return false;
			}

			TomJerrySchedulingService.UpdateStatus status = schedulingService.updateExecution(execution, tick);
			if (status.isCompleted()) {
				executingMouse.completed = true;
				executingMouse = null;
				decisionCooldown = 0;
			}
			return status.isChanged();
		}

		private void render() {
			view.setLayoutX(x - view.getFitWidth() / 2);
			view.setLayoutY(y - view.getFitHeight() + 12);
			view.setScaleX(facing);
			label.setLayoutX(x - 36);
			label.setLayoutY(y + 4);
		}

		private void updateFacing(double dx) {
			if (Math.abs(dx) > 0.2) {
				facing = dx >= 0 ? 1 : -1;
			}
		}
	}

	private final class MouseAgent implements TomJerrySchedulingService.MouseStatus {
		private final Process process;
		private final ImageView view = createImage("/view/image/tomjerry/mouse-crop.png", 29, 24);
		private final Label label = new Label();
		private double x = FALLBACK_SPAWN_X;
		private double y = FALLBACK_SPAWN_Y;
		private double vx;
		private double vy;
		private int facing = 1;
		private boolean spawned;
		private boolean active;
		private boolean caught;
		private boolean completed;

		private MouseAgent(Process process) {
			this.process = process;
			this.label.setText(process.getPid());
			this.label.setAlignment(Pos.CENTER);
			this.label.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 10px; -fx-text-fill: #ffffff; -fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 3; -fx-padding: 1 4;");
			pickDirection();
		}

		private void spawn() {
			spawned = true;
			active = true;
			caught = false;
			double[] spawnPoint = randomFloorPoint();
			x = spawnPoint[0];
			y = spawnPoint[1];
			view.setOpacity(1);
			label.setOpacity(1);
			pickDirection();
		}

		private void update(double delta) {
			avoidNearestCat();
			move(delta);
			render();
		}

		private void move(double delta) {
			double nextX = clamp(x + vx * delta, FLOOR_MIN_X, FLOOR_MAX_X);
			double nextY = clamp(y + vy * delta, FLOOR_MIN_Y, FLOOR_MAX_Y);
			if (isOnFloor(nextX, nextY)) {
				updateFacing(nextX - x);
				x = nextX;
				y = nextY;
			} else {
				vx *= -0.75;
				vy *= -0.75;
				double[] point = randomFloorPoint();
				if (random.nextDouble() < 0.35) {
					vx = point[0] - x;
					vy = point[1] - y;
					normalizeVelocity(58 + random.nextDouble() * 35);
				}
			}

			if (x <= FLOOR_MIN_X + 2 || x >= FLOOR_MAX_X - 2) {
				vx *= -1;
			}
			if (y <= FLOOR_MIN_Y + 2 || y >= FLOOR_MAX_Y - 2) {
				vy *= -1;
			}
			if (random.nextDouble() < 0.015) {
				pickDirection();
			}
		}

		private void avoidNearestCat() {
			CatAgent nearest = null;
			double nearestDistance = Double.MAX_VALUE;
			for (CatAgent cat : cats) {
				double d = distance(x, y, cat.x, cat.y);
				if (d < nearestDistance) {
					nearestDistance = d;
					nearest = cat;
				}
			}

			if (nearest == null || nearestDistance > 112) {
				return;
			}

			double dx = x - nearest.x;
			double dy = y - nearest.y;
			double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
			vx = dx / length * 105;
			vy = dy / length * 105;

		}

		private void pickDirection() {
			double angle = random.nextDouble() * Math.PI * 2;
			double speed = 44 + random.nextDouble() * 42;
			vx = Math.cos(angle) * speed;
			vy = Math.sin(angle) * speed;
			updateFacing(vx);
		}

		private void normalizeVelocity(double speed) {
			double length = Math.max(1, Math.sqrt(vx * vx + vy * vy));
			vx = vx / length * speed;
			vy = vy / length * speed;
		}

		private void hideImmediately() {
			active = false;
			FadeTransition fadeView = new FadeTransition(Duration.millis(520), view);
			fadeView.setToValue(0);
			FadeTransition fadeLabel = new FadeTransition(Duration.millis(520), label);
			fadeLabel.setToValue(0);
			fadeLabel.setOnFinished(event -> world.getChildren().removeAll(view, label));
			fadeView.play();
			fadeLabel.play();
		}

		private void render() {
			view.setLayoutX(x - view.getFitWidth() / 2);
			view.setLayoutY(y - view.getFitHeight());
			view.setScaleX(facing);
			view.setScaleY(1.0);
			label.setLayoutX(x - 12);
			label.setLayoutY(y + 4);
		}

		private void updateFacing(double dx) {
			if (Math.abs(dx) > 0.2) {
				facing = dx >= 0 ? 1 : -1;
			}
		}

		@Override
		public int getArrivalTime() {
			return process.getArrivalTime();
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public boolean isCaught() {
			return caught;
		}

		@Override
		public boolean isCompleted() {
			return completed;
		}
	}

	private enum Mode {
		CHASE,
		WANDER,
		IDLE
	}

	@FunctionalInterface
	interface Listener {
		void onSimulationUpdated(SchedulingResult result);
	}

	private String coreLabel(CoreConfig core, int index) {
		String type = core.getCoreType() == CoreType.P_CORE ? "P-Core" : "E-Core";
		return type + " " + (index + 1);
	}
}
