package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.ux.TransformableNode;

public class CarController {

    private TransformableNode carNode;

    // Sensación general
    private static final float SPEED = 0.025f;
    private static final float MAX_TURN_STEP_DEG = 1.5f;
    private static final float DEADZONE = 0.08f;

    // Nueva lógica de dirección
    private static final float STEER_DEADZONE_X = 0.15f; // si |x| <= esto, no giramos
    private static final float VERT_THRESHOLD_Y = 0.25f; // consideramos "vertical" si |y| >= esto
    private static final float OVERSPIN_BONUS_DEG = 8f;  // sobregiro extra solo en la transición

    // Estado para detectar transición derecha -> vertical
    private boolean wasRight = false;
    private boolean overspinArmed = false;

    public CarController(TransformableNode carNode) {
        this.carNode = carNode;
    }

    public void move(float xPercent, float yPercent) {
        if (carNode == null) return;

        // Magnitud del joystick (salida rápida si está casi centrado)
        float mag = (float) Math.sqrt(xPercent * xPercent + yPercent * yPercent);
        if (mag < DEADZONE) return;

        // --- Detección de zonas ---
        boolean isRight     = xPercent >  STEER_DEADZONE_X;
        boolean isLeft      = xPercent < -STEER_DEADZONE_X;
        boolean isVertical  = Math.abs(xPercent) <= STEER_DEADZONE_X && Math.abs(yPercent) >= VERT_THRESHOLD_Y;

        // --- Transición: derecha -> vertical (armar sobregiro una sola vez) ---
        if (isRight) {
            wasRight = true;
            overspinArmed = false; // mientras sigues a la derecha no armamos nada
        } else if (wasRight && isVertical) {
            overspinArmed = true;  // se arma el sobregiro
            wasRight = false;      // consumiremos en este frame
        } else if (!isVertical) {
            // Cualquier otra zona (izquierda o diagonal), resetea el "venía de derecha"
            wasRight = false;
        }

        // --- Avance lineal (siempre movemos) ---
        float move = SPEED * mag * (yPercent >= 0 ? 1f : -1f);
        Vector3 forwardNow = carNode.getForward();
        Vector3 currentPos = carNode.getLocalPosition();
        Vector3 newPos = Vector3.add(currentPos, forwardNow.scaled(move));
        carNode.setLocalPosition(newPos);

        // --- Rotación ---
        // 1) Si |x| es pequeño (recto), NO giramos... excepto si hay sobregiro armado.
        if (Math.abs(xPercent) <= STEER_DEADZONE_X) {
            if (overspinArmed) {
                // Calculamos rumbo objetivo solo para aplicar el sobregiro una vez
                float targetYawDeg = (float) Math.toDegrees(Math.atan2(xPercent, yPercent));
                float currentYawDeg = getCurrentYawDeg();
                float diff = normalizeAngleDeg(targetYawDeg - currentYawDeg);

                // Paso normal + bonus de sobregiro en el sentido correcto
                float step = clamp(diff, -MAX_TURN_STEP_DEG, +MAX_TURN_STEP_DEG)
                        + Math.signum(diff) * OVERSPIN_BONUS_DEG;

                applyYawStep(step);
                overspinArmed = false; // consumir sobregiro
            }
            // Si no hay sobregiro armado, no rotamos más
            return;
        }

        // 2) Si sí hay intención de giro (|x| > deadzone), giramos normal y suave
        float targetYawDeg = (float) Math.toDegrees(Math.atan2(xPercent, yPercent));
        float currentYawDeg = getCurrentYawDeg();
        float diff = normalizeAngleDeg(targetYawDeg - currentYawDeg);
        float step = clamp(diff, -MAX_TURN_STEP_DEG, +MAX_TURN_STEP_DEG);
        applyYawStep(step);
    }

    public void updateNode(TransformableNode newNode) {
        this.carNode = newNode;
    }

    // --- Helpers ---
    private float getCurrentYawDeg() {
        Vector3 fwd = carNode.getForward();
        return (float) Math.toDegrees(Math.atan2(fwd.x, fwd.z));
    }

    private void applyYawStep(float stepDeg) {
        Quaternion deltaYaw = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), stepDeg);
        Quaternion newRot = Quaternion.multiply(carNode.getLocalRotation(), deltaYaw);
        carNode.setLocalRotation(newRot);
    }

    private static float normalizeAngleDeg(float a) {
        a = (a + 180f) % 360f;
        if (a < 0) a += 360f;
        return a - 180f;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
