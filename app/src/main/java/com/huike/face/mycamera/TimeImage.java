package com.huike.face.mycamera;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * @ProjectName: face_android_device
 * @Package: com.east.face.device.business.main.weight
 * @ClassName: TimeImage
 * @Description: java类作用描述
 * @Author: 谢文良
 * @CreateDate: 2020/4/7 10:20
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/4/7 10:20
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class TimeImage extends AppCompatImageView {
    private Handler timeHandler = new Handler(Looper.getMainLooper(), msg -> {
        setImageDrawable(null);
        return true;
    });

    public TimeImage(Context context) {
        super(context);
    }

    public TimeImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void timing() {
        timeHandler.removeMessages(1);
        timeHandler.sendEmptyMessageDelayed(1, 3000);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        timeHandler.removeMessages(1);
    }
}
