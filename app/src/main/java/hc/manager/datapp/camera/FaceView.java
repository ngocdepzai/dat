package hc.manager.datapp.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Created by wangjingyi on 28/03/2017.
 */

public class FaceView extends View {
    private List<String> ids;
    private List<String> yaws;
    private List<String> pitchs;
    private List<String> rolls;
    private List<String> blurs;
    private List<String> smiles;
    private List<Rect> rect;
    private String rate = "";
    private Paint paint = new Paint();
    private Paint idPaint = new Paint();
    private Paint posePaint = new Paint();
    private Paint backPaint = new Paint();
    private Paint guidePaint = new Paint();
    private boolean showGuide = false;
    private float guideMarginHorizontal = 0.10f;
    private float guideMarginTop = 0.10f;
    private float guideMarginBottom = 0.10f;

    public FaceView(Context context) {
        super(context);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    private void initData() {
        ids = new ArrayList<String>();
        yaws = new ArrayList<>();
        pitchs = new ArrayList<>();
        rolls = new ArrayList<>();
        blurs = new ArrayList<>();
        smiles = new ArrayList<>();
        rect = new ArrayList<Rect>();
        paint.setARGB(122, 255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);

        backPaint.setARGB(122, 255, 255, 255);
        backPaint.setStyle(Paint.Style.FILL);

        idPaint.setARGB(255, 80, 80, 80);
        idPaint.setTextSize(40);

        posePaint.setARGB(255, 80, 80, 80);
        posePaint.setTextSize(25);

        guidePaint.setColor(Color.RED);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(6.0f);
    }

    public void setShowGuide(boolean show) {
        this.showGuide = show;
        postInvalidate();
    }

    public void setGuideMarginTop(float ratio) {
        this.guideMarginTop = ratio;
    }

    public void setGuideMarginHorizontal(float ratio) {
        this.guideMarginHorizontal = ratio;
    }

    public RectF getGuideRect() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return null;
        return new RectF(
                w * guideMarginHorizontal,
                h * guideMarginTop,
                w * (1f - guideMarginHorizontal),
                h * (1f - guideMarginBottom)
        );
    }

    public boolean isInsideGuide(RectF faceRect) {
        RectF g = getGuideRect();
        if (g == null) return false;
        return g.contains(faceRect);
    }

    public void addId(String label) {
        ids.add(label);
    }

    public void addYaw(String label) {
        yaws.add(label);
    }

    public void addPitch(String label) {
        pitchs.add(label);
    }

    public void addRoll(String label) {
        rolls.add(label);
    }

    public void addBlur(String label) {
        blurs.add(label);
    }

    public void addSmile(String lable) {
        smiles.add(lable);
    }

    public void addRect(RectF rect) {
        Rect buffer = new Rect();
        buffer.left = (int) rect.left;
        buffer.top = (int) rect.top;
        buffer.right = (int) rect.right;
        buffer.bottom = (int) rect.bottom;
        this.rect.clear();
        this.rect.add(buffer);
    }

    public void addRate(String rate){
        this.rate = rate;
    }

    public void clear() {
        rect.clear();
        ids.clear();
        yaws.clear();
        rolls.clear();
        blurs.clear();
        pitchs.clear();
        smiles.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showGuide) {
            RectF g = getGuideRect();
            if (g != null) {
                canvas.drawRect(g, guidePaint);
            }
        }
        for (int i = 0; i < rect.size(); i++) {
            if (rect != null) {

                RectF rf = new RectF(rect.get(i));
                canvas.drawRoundRect(rf, 16f, 16f, paint);
                if (!Objects.equals(rate, "")) {
                    canvas.drawText(rate, rf.right + 5, rf.top + 60, posePaint);
                }
            }
            this.clear();
        }
    }
}
