package hc.manager.datapp.utils;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback {
    Camera camera;
    SurfaceHolder holder;

    public ShowCamera(Context context, Camera camera) {
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Camera.Parameters parameters = camera.getParameters();
//        if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
//            parameters.set("orientation","portrait");
//            camera.setDisplayOrientation(90);
//            parameters.setRotation(90);
//        }else{
//            parameters.set("orientation","landscape");
//            camera.setDisplayOrientation(0);
//            parameters.setRotation(0);
//        }
        parameters.set("orientation", "landscape");
        camera.setDisplayOrientation(180);
        parameters.setRotation(180);
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }
}
