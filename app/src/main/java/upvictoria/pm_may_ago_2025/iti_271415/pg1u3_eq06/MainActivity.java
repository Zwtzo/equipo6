package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import android.os.Bundle;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.math.Vector3;
import android.net.Uri;
import android.util.Log;

import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable carRenderable;
    private TransformableNode carNode;
    private String selectedModel = "car_model.glb"; // por defecto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
        Button selectModelButton = findViewById(R.id.btn_select_model);
        selectModelButton.setOnClickListener(v -> showModelPicker());

        loadModel();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (carRenderable == null) return;

            Anchor newAnchor = hitResult.createAnchor();

            if (carNode == null) {
                // 🚗 Primera vez: colocamos el carrito
                AnchorNode anchorNode = new AnchorNode(newAnchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                carNode = new TransformableNode(arFragment.getTransformationSystem());
                carNode.setParent(anchorNode);
                carNode.setRenderable(carRenderable);

                carNode.getScaleController().setMinScale(0.05f);
                carNode.getScaleController().setMaxScale(0.06f);
                carNode.setLocalScale(new Vector3(0.05f, 0.05f, 0.05f));

                carNode.select();

                Log.d("AR", "Carro colocado por primera vez");
            } else {
                // 🔄 Teletransportar el modelo
                AnchorNode newAnchorNode = new AnchorNode(newAnchor);
                newAnchorNode.setParent(arFragment.getArSceneView().getScene());

                // Eliminamos el nodo anterior de la escena
                AnchorNode oldAnchorNode = (AnchorNode) carNode.getParent();
                if (oldAnchorNode != null) {
                    oldAnchorNode.setParent(null);
                }

                // Reasignamos el carrito al nuevo anchor
                carNode.setParent(newAnchorNode);
                carNode.select();

                Log.d("AR", "Carro teletransportado a nueva posición");
            }

        });



        JoystickView joystick = findViewById(R.id.joystick);
        joystick.setJoystickListener((x, y) -> {
            if (carNode != null) {
                Vector3 currentPosition = carNode.getLocalPosition();
                Vector3 newPosition = new Vector3(
                        currentPosition.x + x * 0.05f,
                        currentPosition.y,
                        currentPosition.z - y * 0.05f
                );
                carNode.setLocalPosition(newPosition);
            }
        });

    }

    private void showModelPicker() {
        String[] models = {"car_model.glb", "batimobile.glb"};

        new AlertDialog.Builder(this)
                .setTitle("Selecciona un modelo")
                .setItems(models, (dialog, which) -> {
                    selectedModel = models[which];
                    // Cargar el nuevo modelo y reemplazar si ya hay uno en escena
                    ModelRenderable.builder()
                            .setSource(this, Uri.parse(selectedModel))
                            .setIsFilamentGltf(true)
                            .build()
                            .thenAccept(renderable -> {
                                carRenderable = renderable;
                                if (carNode != null) {
                                    carNode.setRenderable(carRenderable); // reemplazar modelo visualmente
                                    Toast.makeText(this, "Modelo actualizado", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .exceptionally(throwable -> {
                                Toast.makeText(this, "Error cargando el modelo", Toast.LENGTH_SHORT).show();
                                throwable.printStackTrace();
                                return null;
                            });
                })
                .show();
    }


    private void loadModel() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse(selectedModel))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> carRenderable = renderable)
                .exceptionally(throwable -> {
                    Toast.makeText(this, "Error cargando el modelo", Toast.LENGTH_SHORT).show();
                    throwable.printStackTrace();
                    return null;
                });
    }

}
