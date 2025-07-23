package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;

public class CarController {

    private TransformableNode carNode;

    public CarController(TransformableNode carNode) {
        this.carNode = carNode;
    }

    public void move(float xPercent, float yPercent) {
        if (carNode == null) return;

        Vector3 currentPosition = carNode.getLocalPosition();
        // Ajusta la velocidad del movimiento con un factor
        float speed = 0.05f;
        float dx = xPercent * speed;
        float dz = -yPercent * speed; // -y para que arriba en el joystick sea "adelante"

        Vector3 newPosition = new Vector3(
                currentPosition.x + dx,
                currentPosition.y,
                currentPosition.z + dz
        );

        carNode.setLocalPosition(newPosition);
    }

    public void updateNode(TransformableNode newNode) {
        this.carNode = newNode;
    }
}
