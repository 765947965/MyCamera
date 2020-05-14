package com.huike.face.mycamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;


/**
 * @ProjectName: face_android_device
 * @Package: com.east.face.device.business.main.weight
 * @ClassName: BoxView
 * @Description: java类作用描述
 * @Author: 谢文良
 * @CreateDate: 2020/4/14 9:41
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/4/14 9:41
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class BoxView extends View {
    private int mainAngle = 0;
    private int bearAngle = 0;
    private Paint middleCirclePaint;
    private int dp1;
    private int blueColor;
    private int whiteOpen;
    private int defaultColor;
    private int greenColor;
    private int redColor;
    private RectF rectF;


    public BoxView(Context context) {
        super(context);
        init(context);
    }

    public BoxView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BoxView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        defaultColor = Color.BLUE;
        greenColor = Color.GREEN;
        redColor = Color.RED;
        blueColor = defaultColor;
        whiteOpen = Color.parseColor("#35FFFFFF");
        dp1 = 1;

        middleCirclePaint = new Paint();
        middleCirclePaint.setAntiAlias(true);//用于防止边缘的锯齿
        middleCirclePaint.setColor(blueColor);//设置颜色
        middleCirclePaint.setStyle(Paint.Style.STROKE);//设置样式为空心矩形
        middleCirclePaint.setStrokeWidth(dp1 * 2);//设置空心矩形边框的宽度
    }

    public void setBoxGreen() {
        blueColor = greenColor;
    }

    public void setBoxRed() {
        blueColor = redColor;
    }

    public void setBoxDefault() {
        blueColor = defaultColor;
    }

    public void showFaceBox(Rect rect) {
        if (rect != null) {
            // 计算比例(预览宽度/摄像头像素宽度)
            float proportion = ((float) getWidth()) / 480f;
            RectF rectP = new RectF(rect.left * proportion, rect.top * proportion, rect.right * proportion, rect.bottom * proportion);
            // 坐标轴顺时针旋转90度
            float height = getWidth();
            float newLeft = height - rectP.bottom;
            float newRight = height - rectP.top;
            float newTop = rectP.left;
            float newBottom = rectP.right;
            this.rectF = new RectF(newLeft, newTop, newRight, newBottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF temp = rectF;
        if (temp != null) {
            drawBox(canvas, temp);
            mainAngle += 1;
            bearAngle += 1;
            if (mainAngle % 360 == 0) {
                mainAngle = 0;
            }
            if (bearAngle % 360 == 0) {
                bearAngle = 0;
            }
        }
        // 30帧
        postInvalidateDelayed(33);
    }

    private void drawBox(Canvas canvas, RectF temp) {

        RectF rectFinner = new RectF();
        rectFinner.top = temp.top + dp1 * 5f;
        rectFinner.left = temp.left + dp1 * 5f;
        rectFinner.right = temp.right - dp1 * 5f;
        rectFinner.bottom = temp.bottom - dp1 * 5f;

        RectF rectFOuter = new RectF();
        rectFOuter.top = temp.top - dp1 * 5f;
        rectFOuter.left = temp.left - dp1 * 5f;
        rectFOuter.right = temp.right + dp1 * 5f;
        rectFOuter.bottom = temp.bottom + dp1 * 5f;

        RectF rectFinner2 = new RectF();
        rectFinner2.top = temp.top + dp1 * 20f;
        rectFinner2.left = temp.left + dp1 * 20f;
        rectFinner2.right = temp.right - dp1 * 20f;
        rectFinner2.bottom = temp.bottom - dp1 * 20f;

        // 中圈(实线)
        middleCirclePaint.setColor(blueColor);
        middleCirclePaint.setStrokeWidth(dp1 * 16);
        canvas.drawArc(rectF, mainAngle - 90, 90, false, middleCirclePaint);
        canvas.drawArc(rectF, mainAngle + 90, 90, false, middleCirclePaint);
        // 中圈(白虚线)
        middleCirclePaint.setColor(whiteOpen);
        PathEffect dashPathEffect = new DashPathEffect(new float[]{dp1 * 2, dp1 * 2}, 1);
        middleCirclePaint.setPathEffect(dashPathEffect);
        canvas.drawArc(rectF, mainAngle + 180, 90, false, middleCirclePaint);
        // 中圈(双实线)
        middleCirclePaint.setColor(blueColor);
        middleCirclePaint.setPathEffect(null);
        middleCirclePaint.setStrokeWidth(dp1 * 6);
        canvas.drawArc(rectFinner, mainAngle, 90, false, middleCirclePaint);
        canvas.drawArc(rectFOuter, mainAngle, 90, false, middleCirclePaint);
        // 内圈(3段实线)
        //middleCirclePaint.setColor(whiteOpen);
        middleCirclePaint.setStrokeWidth(dp1 * 4);
        canvas.drawArc(rectFinner2, bearAngle - 90, 20, false, middleCirclePaint);
        canvas.drawArc(rectFinner2, bearAngle - 60, 20, false, middleCirclePaint);
        canvas.drawArc(rectFinner2, bearAngle - 30, 20, false, middleCirclePaint);
        canvas.drawArc(rectFinner2, bearAngle + 90, 90, false, middleCirclePaint);
    }
}
