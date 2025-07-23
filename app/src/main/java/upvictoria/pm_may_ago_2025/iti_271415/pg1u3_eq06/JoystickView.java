package upvictoria.pm_may_ago_2025.iti_271415.pg1u3_eq06;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    private float centerX, centerY, baseRadius, hatRadius;
    private float touchX, touchY;
    private JoystickListener listener;

    // ✅ Constructor requerido por XML
    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // ✅ Constructor útil si lo creas desde código
    public JoystickView(Context context) {
        super(context);
        init();
    }

    private void init() {
        touchX = touchY = 0;
    }

    public void setJoystickListener(JoystickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        baseRadius = Math.min(getWidth(), getHeight()) / 3f;
        hatRadius = baseRadius / 2;

        Paint basePaint = new Paint();
        basePaint.setARGB(255, 50, 50, 50);
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);

        Paint hatPaint = new Paint();
        hatPaint.setARGB(255, 200, 0, 0);
        canvas.drawCircle(touchX == 0 ? centerX : touchX, touchY == 0 ? centerY : touchY, hatRadius, hatPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float dx = e.getX() - centerX;
        float dy = e.getY() - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float maxRadius = baseRadius;

        if (distance < maxRadius) {
            touchX = e.getX();
            touchY = e.getY();
        } else {
            touchX = centerX + (dx / distance) * maxRadius;
            touchY = centerY + (dy / distance) * maxRadius;
        }

        invalidate();

        if (listener != null) {
            float xPercent = (touchX - centerX) / maxRadius;
            float yPercent = (touchY - centerY) / maxRadius;
            listener.onJoystickMoved(xPercent, yPercent);
        }

        if (e.getAction() == MotionEvent.ACTION_UP) {
            touchX = centerX;
            touchY = centerY;
            invalidate();
            if (listener != null) listener.onJoystickMoved(0, 0);
        }

        return true;
    }

    public interface JoystickListener {
        void onJoystickMoved(float xPercent, float yPercent);
    }
}
