package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable carRenderable;
    private TransformableNode carNode;
    private CarController controller;

    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private boolean dpadLoopRunning = false;
    private final Handler dpadHandler = new Handler(Looper.getMainLooper());

    private boolean initialSpawnDone = false;

    private final Runnable dpadLoop = new Runnable() {
        @Override
        public void run() {
            if (!dpadLoopRunning) return;
            float x = (rightPressed ? 1f : 0f) + (leftPressed ? -1f : 0f);
            float y = (upPressed ? 1f : 0f) + (downPressed ? -1f : 0f);
            if (controller != null && (x != 0f || y != 0f)) {
                float mag = (float) Math.sqrt(x * x + y * y);
                controller.move(x / mag, y / mag);
            }
            dpadHandler.postDelayed(this, 16);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        loadModel();

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                handleDoubleTap(e);
                return true;
            }
        });

        arFragment.getArSceneView().getScene().setOnTouchListener((hitTestResult, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (initialSpawnDone) return;
            if (carRenderable == null) return;
            if (arFragment.getArSceneView().getArFrame() == null) return;

            Point c = getScreenCenter();
            List<HitResult> hits = arFragment.getArSceneView().getArFrame().hitTest(c.x, c.y);
            for (HitResult hit : hits) {
                Trackable t = hit.getTrackable();
                if (t instanceof Plane) {
                    Plane p = (Plane) t;
                    if (p.getTrackingState() == TrackingState.TRACKING && p.isPoseInPolygon(hit.getHitPose())) {
                        Anchor a = hit.createAnchor();
                        placeOrMoveCar(a);
                        initialSpawnDone = true;
                        Toast.makeText(this, "🚗 Auto colocado automáticamente", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        });

        setupDpad();
    }

    private void loadModel() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("file:///android_asset/batimobile.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    carRenderable = renderable;
                    Toast.makeText(this, "✅ Modelo Batimóvil cargado correctamente", Toast.LENGTH_SHORT).show();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(this, "❌ Error cargando el modelo Batimóvil", Toast.LENGTH_LONG).show();
                    throwable.printStackTrace();
                    return null;
                });
    }

    private Point getScreenCenter() {
        View v = arFragment.getArSceneView();
        return new Point(v.getWidth() / 2, v.getHeight() / 2);
    }

    private void handleDoubleTap(MotionEvent e) {
        if (carRenderable == null) {
            Toast.makeText(this, "Modelo no cargado aún", Toast.LENGTH_SHORT).show();
            return;
        }
        if (arFragment.getArSceneView().getArFrame() == null) {
            Toast.makeText(this, "Frame AR no disponible todavía", Toast.LENGTH_SHORT).show();
            return;
        }
        List<HitResult> hits = arFragment.getArSceneView().getArFrame().hitTest(e);
        if (hits.isEmpty()) {
            Toast.makeText(this, "No se detectó un plano en el doble tap", Toast.LENGTH_SHORT).show();
            return;
        }
        for (HitResult hit : hits) {
            Trackable trackable = hit.getTrackable();
            if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                Anchor newAnchor = hit.createAnchor();
                placeOrMoveCar(newAnchor);
                break;
            }
        }
    }

    private void placeOrMoveCar(Anchor newAnchor) {
        if (carNode == null) {
            AnchorNode anchorNode = new AnchorNode(newAnchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            carNode = new TransformableNode(arFragment.getTransformationSystem());
            carNode.setParent(anchorNode);
            carNode.setRenderable(carRenderable);
            carNode.getScaleController().setMinScale(0.05f);
            carNode.getScaleController().setMaxScale(0.06f);
            carNode.setLocalScale(new Vector3(0.05f, 0.05f, 0.05f));
            carNode.select();

            controller = new CarController(carNode);
            Log.d("AR", "Batimóvil colocado por primera vez");
        } else {
            AnchorNode newAnchorNode = new AnchorNode(newAnchor);
            newAnchorNode.setParent(arFragment.getArSceneView().getScene());
            AnchorNode oldAnchorNode = (AnchorNode) carNode.getParent();
            if (oldAnchorNode != null) {
                oldAnchorNode.setParent(null);
            }
            carNode.setParent(newAnchorNode);
            carNode.select();
            if (controller != null) controller.updateNode(carNode);
            Log.d("AR", "Batimóvil teletransportado a nueva posición");
        }
    }

    private void setupDpad() {
        Button up = findViewById(R.id.btn_up);
        Button down = findViewById(R.id.btn_down);
        Button left = findViewById(R.id.btn_left);
        Button right = findViewById(R.id.btn_right);

        View.OnTouchListener upListener = (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    upPressed = true; startDpadLoop(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    upPressed = false; stopDpadLoopIfIdle(); return true;
            }
            return false;
        };

        View.OnTouchListener downListener = (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downPressed = true; startDpadLoop(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    downPressed = false; stopDpadLoopIfIdle(); return true;
            }
            return false;
        };

        View.OnTouchListener leftListener = (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    leftPressed = true; startDpadLoop(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    leftPressed = false; stopDpadLoopIfIdle(); return true;
            }
            return false;
        };

        View.OnTouchListener rightListener = (v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    rightPressed = true; startDpadLoop(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    rightPressed = false; stopDpadLoopIfIdle(); return true;
            }
            return false;
        };

        up.setOnTouchListener(upListener);
        down.setOnTouchListener(downListener);
        left.setOnTouchListener(leftListener);
        right.setOnTouchListener(rightListener);
    }

    private void startDpadLoop() {
        if (!dpadLoopRunning) {
            dpadLoopRunning = true;
            dpadHandler.post(dpadLoop);
        }
    }

    private void stopDpadLoopIfIdle() {
        if (!upPressed && !downPressed && !leftPressed && !rightPressed) {
            dpadLoopRunning = false;
        }
    }
}
