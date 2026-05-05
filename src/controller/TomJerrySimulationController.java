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
import model.Process;
import model.SchedulingResult;
import model.TomJerryVector;
import service.tomjerry.TomJerryPhysicsService;
import service.tomjerry.TomJerryRoom;
import service.tomjerry.TomJerrySchedulingService;
import service.tomjerry.TomJerrySpriteConfig;

final class TomJerrySimulationController {

	private static final double SIM_SPEED = 1.35;

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

	// Tom & Jerry 전용 창을 새로 연다. 이 알고리즘은 메인 Gantt만 그리는 방식이 아니라
	// 별도 애니메이션 창에서 고양이가 생쥐를 잡는 순서로 실행 순서가 결정된다.
	void show(List<Process> processes, List<CoreConfig> cores, Listener listener) {
		closePreviousStage();
		this.listener = listener;
		stage = new Stage();
		stage.setTitle("Tom & Jerry Scheduling");
		stage.setResizable(true);

		StackPane root = new StackPane(world);
		root.setStyle("-fx-background-color: #101010;");
		Scene scene = new Scene(root, TomJerryRoom.WIDTH, TomJerryRoom.HEIGHT, Color.web("#101010"));
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

	// 새 실행을 시작하기 전에 이전 창의 AnimationTimer를 반드시 멈춘다.
	// 타이머가 남아 있으면 닫힌 창에서도 Gantt 갱신 콜백이 계속 호출될 수 있다.
	private void closePreviousStage() {
		stopLoop();
		if (stage != null) {
			stage.close();
			stage = null;
		}
	}

	// 방 배경, 제목, 시간 라벨처럼 움직이지 않는 화면 요소를 배치한다.
	private void setupWorld() {
		world.getChildren().clear();
		world.setPrefSize(TomJerryRoom.WIDTH, TomJerryRoom.HEIGHT);
		world.setStyle("-fx-background-color: #101010;");

		ImageView room = createImage("/view/image/room.png", TomJerryRoom.ROOM_WIDTH, TomJerryRoom.ROOM_HEIGHT);
		room.setLayoutX(TomJerryRoom.ROOM_LEFT);
		room.setLayoutY(TomJerryRoom.ROOM_TOP);
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

	// 프로세스를 도착 시간순 생쥐 에이전트로 변환한다.
	// 실제 등장은 arrivalTime에 맞춰 spawnArrivals에서 처리한다.
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

	// 선택된 Core만 고양이 에이전트로 만든다.
	// P-Core/E-Core 속도와 이미지 차이는 TomJerrySpriteConfig에서 가져온다.
	private void setupCores(List<CoreConfig> cores) {
		cats.clear();
		int visibleIndex = 0;

		for (CoreConfig core : cores) {
			if (!core.isEnabled()) {
				continue;
			}

			TomJerryVector point = TomJerryRoom.randomFloorPoint(random);
			double x = point.x();
			double y = point.y();
			CatAgent cat = new CatAgent(core, visibleIndex, x, y);
			cats.add(cat);
			world.getChildren().addAll(cat.view, cat.label);
			cat.render();
			visibleIndex++;
		}
	}

	// AnimationTimer는 화면 프레임마다 호출된다. delta는 실제 프레임 간격이고,
	// elapsedSimTime은 SIM_SPEED를 곱해 스케줄링 tick으로 변환되는 내부 시간이다.
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

	// 실행 중인 애니메이션 루프를 중지한다.
	private void stopLoop() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
	}

	// 프레임마다 캐릭터 이동은 갱신하지만, 프로세스 도착/실행 진행은 정수 tick이 바뀔 때만 처리한다.
	// 과제 조건의 "1초 단위 스케줄링"을 애니메이션과 분리하기 위한 구조다.
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

	// Arrival Time이 된 프로세스를 생쥐로 화면에 등장시킨다.
	private void spawnArrivals(int tick) {
		for (MouseAgent mouse : mice) {
			if (!mouse.spawned && mouse.process.getArrivalTime() <= tick) {
				mouse.spawn();
				world.getChildren().addAll(mouse.view, mouse.label);
				mouse.render();
			}
		}
	}

	// 서비스의 랜덤 선택 규칙에 따라 고양이가 쫓을 생쥐를 고른다.
	private MouseAgent chooseTargetFor(CatAgent cat) {
		return schedulingService.chooseTarget(cat.target, mice, random);
	}

	// 고양이가 이미 생쥐를 잡아 실행 중인 경우 tick 진행에 맞춰 Gantt 블록을 늘린다.
	// 변경이 있었을 때만 메인 화면으로 결과를 발행해 불필요한 redraw를 줄인다.
	private void updateBusyCats(int tick) {
		boolean changed = false;
		for (CatAgent cat : cats) {
			changed |= cat.updateExecution(tick);
		}
		if (changed) {
			publishResult();
		}
	}

	// 모든 생쥐 프로세스가 완료됐는지 확인한다.
	private boolean allMiceCompleted() {
		return schedulingService.allCompleted(mice);
	}

	// AnimationTimer handle 안에서 showAndWait를 호출하면 JavaFX 예외가 나므로 runLater로 미룬다.
	// completionShown은 완료 알림이 여러 번 예약되는 것을 막는다.
	private void showCompletionDialog() {
		if (completionShown) {
			return;
		}

		completionShown = true;
		Platform.runLater(() -> dialogController.showWarning(
				"Simulation Complete",
				"Tom & Jerry Scheduling이 완료되었습니다."));
	}

	// 픽셀 이미지가 흐려지지 않도록 ImageView를 생성한다.
	private ImageView createImage(String path, double fitWidth, double fitHeight) {
		ImageView view = new ImageView(new Image(getClass().getResource(path).toExternalForm()));
		view.setFitWidth(fitWidth);
		view.setFitHeight(fitHeight);
		view.setPreserveRatio(true);
		view.setSmooth(false);
		return view;
	}

	// 메인 화면에 현재까지의 Tom & Jerry Gantt 결과를 전달한다.
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

		// Core 설정에 맞는 고양이 이미지, 이동 속도, 포획 반경, 라벨을 초기화한다.
		private CatAgent(CoreConfig core, int index, double x, double y) {
			this.core = core;
			this.coreIndex = index;
			this.x = x;
			this.y = y;
			this.wanderX = x;
			this.wanderY = y;
			this.speed = TomJerrySpriteConfig.catSpeed(core.getCoreType());
			this.captureRadius = TomJerrySpriteConfig.catCaptureRadius(core.getCoreType());
			this.view = createImage(
					TomJerrySpriteConfig.catImagePath(core.getCoreType()),
					TomJerrySpriteConfig.catWidth(core.getCoreType()),
					TomJerrySpriteConfig.catHeight(core.getCoreType()));
			this.label = new Label(TomJerrySpriteConfig.coreLabel(core.getCoreType(), index));
			this.label.setAlignment(Pos.CENTER);
			this.label.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 11px; -fx-text-fill: #f2f2f2; -fx-background-color: rgba(20,20,20,0.6); -fx-background-radius: 4; -fx-padding: 2 6;");
		}

		// 고양이는 실행 중이면 움직이지 않고, 아니면 일정 간격마다 다음 행동을 다시 결정한다.
		// CHASE/WANDER/IDLE 모드가 Tom & Jerry 알고리즘의 랜덤성을 만든다.
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

		// 대부분은 생쥐를 추격하지만, 낮은 확률로 배회하거나 멈춰서 예능형 알고리즘 느낌을 만든다.
		// target을 가끔 다시 고르는 이유는 같은 생쥐만 계속 따라가는 것을 피하기 위해서다.
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
				TomJerryVector point = TomJerryRoom.randomFloorPoint(random);
				wanderX = point.x();
				wanderY = point.y();
			} else {
				mode = Mode.IDLE;
				wanderX = x;
				wanderY = y;
			}
		}

		// 목표 좌표를 향해 이동하되 방 바닥 밖으로 나가지 않게 제한한다.
		private void moveToward(double targetX, double targetY, double delta) {
			if (mode == Mode.IDLE) {
				return;
			}

			TomJerryVector next = TomJerryPhysicsService.moveToward(x, y, targetX, targetY, speed, delta);
			if (next.x() == x && next.y() == y) {
				return;
			}

			if (TomJerryRoom.isOnFloor(next.x(), next.y())) {
				updateFacing(next.x() - x);
				x = next.x();
				y = next.y();
			} else {
				TomJerryVector point = TomJerryRoom.randomFloorPoint(random);
				wanderX = point.x();
				wanderY = point.y();
				mode = Mode.WANDER;
			}
		}

		// 포획은 "프로세스가 Core에 배정되는 순간"에 해당한다.
		// 잡힌 생쥐는 화면에서 사라지고, 해당 Core는 실행이 끝날 때까지 다른 생쥐를 쫓지 않는다.
		private void tryCapture() {
			if (target == null || target.completed || !target.active) {
				return;
			}
			if (TomJerryRoom.distance(x, y, target.x, target.y) > captureRadius) {
				return;
			}

			startExecution(target);
			target = null;
		}

		// 포획된 생쥐를 프로세스 실행으로 전환한다.
		// P-Core/E-Core 성능 차이에 따른 실행 시간 계산은 TomJerrySchedulingService가 담당한다.
		private void startExecution(MouseAgent mouse) {
			mouse.caught = true;
			mouse.hideImmediately();
			executingMouse = mouse;
			mode = Mode.IDLE;
			if (schedulingService.startExecution(execution, mouse.process, core, coreIndex, previousTick)) {
				publishResult();
			}
		}

		// 실행 중인 프로세스의 tick 진행을 서비스에 위임한다.
		// 완료되면 고양이를 다시 추격 가능한 상태로 돌린다.
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

		// 고양이 이미지와 라벨의 화면 위치를 현재 좌표에 맞춘다.
		private void render() {
			view.setLayoutX(x - view.getFitWidth() / 2);
			view.setLayoutY(y - view.getFitHeight() + 12);
			view.setScaleX(facing);
			label.setLayoutX(x - 36);
			label.setLayoutY(y + 4);
		}

		// 이동 방향에 따라 고양이 이미지를 좌우 반전해 바라보는 방향을 맞춘다.
		private void updateFacing(double dx) {
			if (Math.abs(dx) > 0.2) {
				facing = dx >= 0 ? 1 : -1;
			}
		}
	}

	private final class MouseAgent implements TomJerrySchedulingService.MouseStatus {
		private final Process process;
		private final ImageView view = createImage(
				"/view/image/mouse-crop.png",
				TomJerrySpriteConfig.MOUSE_WIDTH,
				TomJerrySpriteConfig.MOUSE_HEIGHT);
		private final Label label = new Label();
		private double x = TomJerryRoom.FALLBACK_SPAWN_X;
		private double y = TomJerryRoom.FALLBACK_SPAWN_Y;
		private double vx;
		private double vy;
		private int facing = 1;
		private boolean spawned;
		private boolean active;
		private boolean caught;
		private boolean completed;

		// 프로세스를 생쥐 에이전트로 감싸고, 화면에 표시할 PID 라벨을 준비한다.
		private MouseAgent(Process process) {
			this.process = process;
			this.label.setText(process.getPid());
			this.label.setAlignment(Pos.CENTER);
			this.label.setStyle("-fx-font-family: 'Noto Sans'; -fx-font-size: 10px; -fx-text-fill: #ffffff; -fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 3; -fx-padding: 1 4;");
			pickDirection();
		}

		// 도착한 프로세스를 방 바닥의 랜덤 위치에 생쥐로 등장시킨다.
		private void spawn() {
			spawned = true;
			active = true;
			caught = false;
			TomJerryVector spawnPoint = TomJerryRoom.randomFloorPoint(random);
			x = spawnPoint.x();
			y = spawnPoint.y();
			view.setOpacity(1);
			label.setOpacity(1);
			pickDirection();
		}

		// 생쥐는 계속 배회하다가 가까운 고양이가 있으면 회피 속도로 바꾼다.
		// 점프 회피는 제거했고, 현재는 바닥 위 이동만 사용한다.
		private void update(double delta) {
			avoidNearestCat();
			move(delta);
			render();
		}

		// 생쥐가 방 바닥 다각형 밖으로 나가려 하면 튕기거나 새 바닥 좌표 쪽으로 방향을 바꾼다.
		// 화면 밖/벽 위로 올라가는 문제를 막기 위한 보정이다.
		private void move(double delta) {
			TomJerryVector next = TomJerryPhysicsService.moveByVelocity(x, y, vx, vy, delta);
			if (TomJerryRoom.isOnFloor(next.x(), next.y())) {
				updateFacing(next.x() - x);
				x = next.x();
				y = next.y();
			} else {
				TomJerryVector bounced = TomJerryPhysicsService.bouncedVelocity(vx, vy);
				vx = bounced.x();
				vy = bounced.y();
				TomJerryVector point = TomJerryRoom.randomFloorPoint(random);
				if (random.nextDouble() < 0.35) {
					TomJerryVector redirected = TomJerryPhysicsService.redirectMouseVelocity(x, y, point, random);
					vx = redirected.x();
					vy = redirected.y();
				}
			}

			if (random.nextDouble() < 0.015) {
				pickDirection();
			}
		}

		// 가까운 고양이가 있으면 반대 방향으로 도망가도록 속도를 바꾼다.
		private void avoidNearestCat() {
			CatAgent nearest = null;
			double nearestDistance = Double.MAX_VALUE;
			for (CatAgent cat : cats) {
				double d = TomJerryRoom.distance(x, y, cat.x, cat.y);
				if (d < nearestDistance) {
					nearestDistance = d;
					nearest = cat;
				}
			}

			if (nearest == null || !TomJerryPhysicsService.shouldAvoidCat(nearestDistance)) {
				return;
			}

			TomJerryVector velocity = TomJerryPhysicsService.escapeVelocity(x, y, nearest.x, nearest.y);
			vx = velocity.x();
			vy = velocity.y();

		}

		// 생쥐가 다음에 움직일 무작위 방향과 속도를 새로 정한다.
		private void pickDirection() {
			TomJerryVector velocity = TomJerryPhysicsService.randomMouseVelocity(random);
			vx = velocity.x();
			vy = velocity.y();
			updateFacing(vx);
		}

		// 고양이에게 잡힌 생쥐를 fade-out 후 화면에서 제거한다.
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

		// 생쥐 이미지와 라벨의 화면 위치를 현재 좌표에 맞춘다.
		private void render() {
			view.setLayoutX(x - view.getFitWidth() / 2);
			view.setLayoutY(y - view.getFitHeight());
			view.setScaleX(facing);
			view.setScaleY(1.0);
			label.setLayoutX(x - 12);
			label.setLayoutY(y + 4);
		}

		// 이동 방향에 따라 생쥐 이미지를 좌우 반전해 바라보는 방향을 맞춘다.
		private void updateFacing(double dx) {
			if (Math.abs(dx) > 0.2) {
				facing = dx >= 0 ? 1 : -1;
			}
		}

		@Override
		// 스케줄링 서비스가 arrivalTime 기준으로 등장 가능 여부를 판단할 때 사용한다.
		public int getArrivalTime() {
			return process.getArrivalTime();
		}

		@Override
		// 스케줄링 서비스가 현재 화면에 등장한 생쥐만 대상으로 고를 수 있게 한다.
		public boolean isActive() {
			return active;
		}

		@Override
		// 이미 고양이에게 잡힌 생쥐가 다시 타깃이 되지 않도록 상태를 알려준다.
		public boolean isCaught() {
			return caught;
		}

		@Override
		// 실행이 끝난 프로세스가 다시 스케줄링되지 않도록 완료 상태를 알려준다.
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
		// Tom & Jerry 창에서 계산된 중간 결과를 메인 Gantt/Table/Overview에 전달한다.
		void onSimulationUpdated(SchedulingResult result);
	}

}
