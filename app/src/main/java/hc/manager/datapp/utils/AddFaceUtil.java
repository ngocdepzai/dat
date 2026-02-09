//package hc.manager.datapp.utils;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.os.Environment;
//import android.text.TextUtils;
//import android.util.Log;
//import android.widget.Toast;
//
//import java.util.concurrent.ExecutionException;
//
//import hc.manager.datapp.activity.FaceActivity;
//import hc.manager.datapp.service.GetBitmapFromUrl;
//import mcv.facepass.FacePassException;
//import mcv.facepass.FacePassHandler;
//import mcv.facepass.types.FacePassAddFaceResult;
//import mcv.facepass.types.FacePassConfig;
//import mcv.facepass.types.FacePassModel;
//import mcv.facepass.types.FacePassPose;
//
//public class AddFaceUtil {
//    private Context _context;
//    private String DEBUG_TAG = "UPDATE_AVATAR";
//    private String _fileRootPath;
//    private boolean isLocalGroupExist = false;
//    private boolean ageGenderEnabledGlobal;
//    private String _resource;
//    public AddFaceUtil(String resource){
//        _resource = resource;
//    }
//    private void checkGroup() {
//        if (FacePassHandlerUtil.mFacePassHandler == null) {
//            return;
//        }
//        try {
//            String[] localGroups = FacePassHandlerUtil.mFacePassHandler.getLocalGroups();
//            isLocalGroupExist = false;
////            if (localGroups == null || localGroups.length == 0) {
////                faceView.post(new Runnable() {
////                    @Override
////                    public void run() {
////                        toast("Please create" + group_name + "Base library");
////                    }
////                });
////                return;
////            }
////            for (String group : localGroups) {
////                if (group_name.equals(group)) {
////                    isLocalGroupExist = true;
////                }
////            }
////            if (!isLocalGroupExist) {
////                faceView.post(new Runnable() {
////                    @Override
////                    public void run() {
////                        toast("Please create" + group_name + "Base library");
////                    }
////                });
////            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    public void AddFace(String resource) {
//        Bitmap bitmap = null;
//        try {
//            bitmap = new GetBitmapFromUrl(resource).execute().get();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            FacePassAddFaceResult result = FacePassHandlerUtil.mFacePassHandler.addFace(bitmap);
//            if (result != null) {
//                if (result.result == 0) {
//                    if (FacePassHandlerUtil.mFacePassHandler == null) {
//                        toast("FacePassHandle is null ! ");
//                        return;
//                    }
//                    byte[] faceToken = new String(result.faceToken).toString().getBytes();
//                    String groupName = "facepass";
//                    if (faceToken == null || faceToken.length == 0 || TextUtils.isEmpty(groupName)) {
//                        toast("params error！");
//                        return;
//                    }
//                    try {
//                        boolean b = FacePassHandlerUtil.mFacePassHandler.bindGroup(groupName, faceToken);
//                        String result2 = b ? "success " : "failed";
//                        toast("bind  " + result2);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        toast(e.getMessage());
//                    }
//                } else if (result.result == 1) {
//                    Log.d(DEBUG_TAG,  ": no face" );
//                } else {
//                    Log.d(DEBUG_TAG,  ": uality problem" );
//                }
//            }
//        } catch (FacePassException e) {
//            e.printStackTrace();
//        }
//    }
//    private void toast(String msg) {
//       Log.d(DEBUG_TAG,msg);
//    }
//    private void initFaceHandler() {
//        new Thread() {
//            @Override
//            public void run() {
//                while (true) {
//                    while (FacePassHandler.isAvailable()) {
//                        Log.d(DEBUG_TAG, "start to build FacePassHandler");
//                        FacePassConfig config;
//                        try {
//                            /* 填入所需要的模型配置 */
//                            config = new FacePassConfig();
//                            config.poseBlurModel = FacePassModel.initModel(_context.getAssets(), "attr.pose_blur.arm.190630.bin");
//
//                            config.livenessModel = FacePassModel.initModel(_context.getAssets(), "liveness.CPU.rgb.G.bin");
//
//                            config.searchModel = FacePassModel.initModel(_context.getAssets(), "feat2.arm.J2.v1.0_1core.bin");
//
//                            config.detectModel = FacePassModel.initModel(_context.getAssets(), "detector.arm.G.bin");
//                            config.detectRectModel = FacePassModel.initModel(_context.getAssets(), "detector_rect.arm.G.bin");
//                            config.landmarkModel = FacePassModel.initModel(_context.getAssets(), "pf.lmk.arm.E.bin");
//
//                            config.rcAttributeModel = FacePassModel.initModel(_context.getAssets(), "attr.RC.arm.E.bin");
//                            config.occlusionFilterModel = FacePassModel.initModel(_context.getAssets(), "attr.occlusion.arm.20201209.bin");
//                            //config.smileModel = FacePassModel.initModel(getApplicationContext().getAssets(), "attr.RC.arm.200815.bin");
//                            //config.ageGenderModel = FacePassModel.initModel(getApplicationContext().getAssets(), "attr.age_gender.arm.190630.bin");
//
//                            /* 送识别阈值参数 */
//                            config.rcAttributeAndOcclusionMode = 3;
//                            config.searchThreshold = 65f;
//                            config.livenessThreshold = 55f;
//                            config.livenessEnabled = true;
//                            config.rgbIrLivenessEnabled = false;
//                            ageGenderEnabledGlobal = (config.ageGenderModel != null);
//
//                            config.poseThreshold = new FacePassPose(35f, 35f, 35f);
//                            config.blurThreshold = 0.8f;
//                            config.lowBrightnessThreshold = 30f;
//                            config.highBrightnessThreshold = 210f;
//                            config.brightnessSTDThreshold = 80f;
//                            config.faceMinThreshold = 100;
//                            config.retryCount = 10;
//                            config.smileEnabled = false;
//                            config.maxFaceEnabled = true;
//
//                            config.fileRootPath = _fileRootPath;
//
//                            /* 创建SDK实例 */
//                            FacePassHandlerUtil.mFacePassHandler = new FacePassHandler(config);
//
//                            /* 入库阈值参数 */
//                            FacePassConfig addFaceConfig = FacePassHandlerUtil.mFacePassHandler.getAddFaceConfig();
//                            addFaceConfig.poseThreshold.pitch = 35f;
//                            addFaceConfig.poseThreshold.roll = 35f;
//                            addFaceConfig.poseThreshold.yaw = 35f;
//                            addFaceConfig.blurThreshold = 0.7f;
//                            addFaceConfig.lowBrightnessThreshold = 70f;
//                            addFaceConfig.highBrightnessThreshold = 220f;
//                            addFaceConfig.brightnessSTDThreshold = 60f;
//                            addFaceConfig.faceMinThreshold = 100;
//                            addFaceConfig.rcAttributeAndOcclusionMode = 2;
//                            FacePassHandlerUtil.mFacePassHandler.setAddFaceConfig(addFaceConfig);
//
//                            checkGroup();
//                        } catch (FacePassException e) {
//                            e.printStackTrace();
//                            Log.d(DEBUG_TAG, "FacePassHandler is null");
//                            return;
//                        }
//                        return;
//                    }
//                    try {
//                        /* 如果SDK初始化未完成则需等待 */
//                        sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();
//    }
//
//}
