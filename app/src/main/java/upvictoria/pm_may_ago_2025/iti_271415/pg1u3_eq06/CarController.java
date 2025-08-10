package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.ux.TransformableNode;

public class CarController {

    private TransformableNode carNode;

    // Ajustes de “sensación” (más lento/suave)
    private static final float SPEED = 0.025f;           // velocidad lineal (más lento)
    private static final float MAX_TURN_STEP_DEG = 1.5f; // giro máximo por tick en grados (suave)
    private static final float DEADZONE = 0.08f;         // zona muerta del joystick

    public CarController(TransformableNode carNode) {
        this.carNode = carNode;
    }

    public void move(float xPercent, float yPercent) {
        if (carNode == null) return;

        // Magnitud del joystick
        float mag = (float) Math.sqrt(xPercent * xPercent + yPercent * yPercent);
        if (mag < DEADZONE) {
            // Joystick casi centrado: no rotamos ni avanzamos
            return;
        }

        // ===== 1) Rumbo objetivo a partir del joystick =====
        // y>0 = adelante (0°), x>0 = derecha (+)
        float targetYawDeg = (float) Math.toDegrees(Math.atan2(xPercent, yPercent));

        // ===== 2) Rumbo actual del coche =====
        // getForward() da la dirección hacia donde mira. Convertimos a yaw.
        Vector3 fwd = carNode.getForward();
        float currentYawDeg = (float) Math.toDegrees(Math.atan2(fwd.x, fwd.z));

        // ===== 3) Diferencia y normalización al rango [-180, 180] =====
        float diff = normalizeAngleDeg(targetYawDeg - currentYawDeg);

        // Limitar la rotación por tick para que gire suave
        float step = clamp(diff, -MAX_TURN_STEP_DEG, +MAX_TURN_STEP_DEG);

        // Aplicar rotación incremental alrededor del eje Y
        Quaternion deltaYaw = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), step);
        Quaternion newRot = Quaternion.multiply(carNode.getLocalRotation(), deltaYaw);
        carNode.setLocalRotation(newRot);

        // ===== 4) Avance hacia adelante (dirección ya alineada gradualmente) =====
        // Escalamos por magnitud del joystick para sentir más/menos acelerador.
        float move = SPEED * mag * (yPercent >= 0 ? 1f : -1f); // hacia atrás si y<0
        Vector3 forwardNow = carNode.getForward();
        Vector3 currentPos = carNode.getLocalPosition();
        Vector3 newPos = Vector3.add(currentPos, forwardNow.scaled(move));
        carNode.setLocalPosition(newPos);
    }

    public void updateNode(TransformableNode newNode) {
        this.carNode = newNode;
    }

    // --- Helpers ---
    private static float normalizeAngleDeg(float a) {
        // Normaliza a [-180, 180]
        a = (a + 180f) % 360f;
        if (a < 0) a += 360f;
        return a - 180f;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
