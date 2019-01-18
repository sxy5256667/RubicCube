/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.R; // Explicit import needed for internal Google builds.

public abstract class CameraActivity extends Activity
    implements OnImageAvailableListener, Camera.PreviewCallback {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private boolean debug = false;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private final String[] action = { "fist:0", "up:1", "left:2",  "palm:3", "down:4", "right:5"};
    List<HashMap<String,String>> aList;
    GridView actionGrid;

    TextView front1, front2, front3, front4;
    TextView back1, back2, back3, back4;
    TextView up1, up2, up3, up4;
    TextView down1, down2, down3, down4;
    TextView left1, left2, left3, left4;
    TextView right1, right2, right3, right4;
    TextView up, front, left;
    TextView down, back, right;

    boolean selected = false;
    int x = 0, y = 0;

    private Handler mHandler;
    private RubicCube cube;

    private int option;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        cube = new RubicCube();
        // Each row in the list stores country name, currency and flag
        aList = new ArrayList<HashMap<String,String>>();
        for(int i=0;i<6;i++){
            HashMap<String, String> hm = new HashMap<String,String>();
            hm.put("txt", action[i]);
            aList.add(hm);
        }
        String[] from = { "txt"};
        int[] to = {R.id.txt};
        SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), aList, R.layout.action_item, from, to);

        actionGrid = (GridView) findViewById(R.id.action);
        actionGrid.setAdapter(adapter);

        actionGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        break;
                    case 1:
                        if(selected){
                                cube.twist_up(x);
                        }else{
                            if(y ==0){
                                cube.resetHover();
                                cube.push_down();
                                y = 1;
                                cube.setHover(x,y);
                            }else{
                                cube.resetHover();
                                y = 0;
                                cube.setHover(x,y);
                            }
                        }
                        refresh();
                        break;
                    case 2:
                        if(selected){
                            cube.twist_left(y);
                        }else{
                            if(x ==0){
                                cube.resetHover();
                                cube.push_right();
                                x = 1;
                                cube.setHover(x,y);
                            }else{
                                cube.resetHover();
                                x = 0;
                                cube.setHover(x,y);
                            }
                        }
                        refresh();
                        break;
                    case 3:
                        if(selected){
                            cube.setHover(x,y);
                            setSelected(x,y, selected);
                            selected = false;
                        }else{
                            cube.resetHover();
                            setSelected(x,y, selected);
                            selected = true;
                        }
                        refresh();
                        break;
                    case 4:
                        if(selected){
                            cube.twist_down(x);
                        }else{
                            if(y ==1){
                                cube.resetHover();
                                cube.push_up();
                                y = 0;
                                cube.setHover(x,y);
                            }else{
                                cube.resetHover();
                                y = 1;
                                cube.setHover(x,y);
                            }
                        }
                        refresh();
                        break;
                    case 5:
                        if(selected){
                            cube.twist_right(y);
                        }else{
                            if(x ==1){
                                cube.resetHover();
                                cube.push_left();
                                x = 0;
                                cube.setHover(x,y);
                            }else{
                                cube.resetHover();
                                x = 1;
                                cube.setHover(x,y);
                            }
                        }
                        refresh();
                        break;
                }
            }
        });
        front1 = (TextView) findViewById(R.id.front1);
        front2 = (TextView) findViewById(R.id.front2);
        front3 = (TextView) findViewById(R.id.front3);
        front4 = (TextView) findViewById(R.id.front4);
        back1 = (TextView) findViewById(R.id.back1);
        back2 = (TextView) findViewById(R.id.back2);
        back3 = (TextView) findViewById(R.id.back3);
        back4 = (TextView) findViewById(R.id.back4);
        up1 = (TextView) findViewById(R.id.up1);
        up2 = (TextView) findViewById(R.id.up2);
        up3 = (TextView) findViewById(R.id.up3);
        up4 = (TextView) findViewById(R.id.up4);
        down1 = (TextView) findViewById(R.id.down1);
        down2 = (TextView) findViewById(R.id.down2);
        down3 = (TextView) findViewById(R.id.down3);
        down4 = (TextView) findViewById(R.id.down4);
        left1 = (TextView) findViewById(R.id.left1);
        left2 = (TextView) findViewById(R.id.left2);
        left3 = (TextView) findViewById(R.id.left3);
        left4 = (TextView) findViewById(R.id.left4);
        right1 = (TextView) findViewById(R.id.right1);
        right2 = (TextView) findViewById(R.id.right2);
        right3 = (TextView) findViewById(R.id.right3);
        right4 = (TextView) findViewById(R.id.right4);

        up = (TextView) findViewById(R.id.up);
        front = (TextView) findViewById(R.id.front);
        left = (TextView) findViewById(R.id.left);
        down = (TextView) findViewById(R.id.down);
        back = (TextView) findViewById(R.id.back);
        right = (TextView) findViewById(R.id.right);

        mHandler = new Handler();
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        refresh();
    }

    protected void setMotion(int op){
            switch (op) {
                case 0:
                    option = 0;
                    refresh();
                    break;
                case 1:
                    if(option!=1){
                        option = 1;
                        if (selected) {
                            cube.twist_up(x);
                        } else {
                            if (y == 0) {
                                cube.resetHover();
                                cube.push_down();
                                y = 1;
                                cube.setHover(x, y);
                            } else {
                                cube.resetHover();
                                y = 0;
                                cube.setHover(x, y);
                            }
                        }
                    }
                    refresh();
                    break;
                case 2:
                    if(option!=2){
                        option = 2;
                        if (selected) {
                            cube.twist_left(y);
                        } else {
                            if (x == 0) {
                                cube.resetHover();
                                cube.push_right();
                                x = 1;
                                cube.setHover(x, y);
                            } else {
                                cube.resetHover();
                                x = 0;
                                cube.setHover(x, y);
                            }
                        }
                    }
                    refresh();
                    break;
                case 3:
                    if(option!=3){
                        option = 3;
                        if (selected) {
                            cube.setHover(x, y);
                            setSelected(x, y, selected);
                            selected = false;
                        } else {
                            cube.resetHover();
                            setSelected(x, y, selected);
                            selected = true;
                        }
                    }
                    refresh();
                    break;
                case 4:
                    if(option!=4){
                        option = 4;
                        if (selected) {
                            cube.twist_down(x);
                        } else {
                            if (y == 1) {
                                cube.resetHover();
                                cube.push_up();
                                y = 0;
                                cube.setHover(x, y);
                            } else {
                                cube.resetHover();
                                y = 1;
                                cube.setHover(x, y);
                            }
                        }
                    }
                    refresh();
                    break;
                case 5:
                    if(option!=5){
                        option = 5;
                        if (selected) {
                            cube.twist_right(y);
                        } else {
                            if (x == 1) {
                                cube.resetHover();
                                cube.push_left();
                                x = 0;
                                cube.setHover(x, y);
                            } else {
                                cube.resetHover();
                                x = 1;
                                cube.setHover(x, y);
                            }
                        }14
                    }
                    refresh();
                    break;
            }
    }

    private byte[] lastPreviewFrame;

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }


    protected int getLuminanceStride() {
      return yRowStride;
    }

    protected byte[] getLuminance() {
      return yuvBytes[0];
    }

    public void setRecognized(final int i){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                actionGrid.getChildAt(i).setBackgroundColor(Color.RED);
            }
        });
    }

    public void setUnrecognized(final int i){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for(int j=0; j<6; j++){
                    if(j != i){
                        actionGrid.getChildAt(j).setBackgroundColor(Color.WHITE);
                    }
                }
            }
        });
    }

    public void setSelected(final int x, final int y, final boolean Selected){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(!Selected){
                    Animation anim = new ScaleAnimation(
                            1f, 0.9f, // Start and end values for the X axis scaling
                            1f, 0.9f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(100);
                    if(x == 0){
                        if(y == 0){
                            front1.startAnimation(anim);
                        }else{
                            front3.startAnimation(anim);
                        }
                    }else{
                        if(y == 0){
                            front2.startAnimation(anim);
                        }else{
                            front4.startAnimation(anim);
                        }
                    }

                }else{
                    Animation anim = new ScaleAnimation(
                            0.9f,1f, // Start and end values for the X axis scaling
                            0.9f, 1f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(100);
                    if(x == 0){
                        if(y == 0){
                            front1.startAnimation(anim);
                        }else{
                            front3.startAnimation(anim);
                        }
                    }else{
                        if(y == 0){
                            front2.startAnimation(anim);
                        }else{
                            front4.startAnimation(anim);
                        }
                    }
                }

            }
        });
    }

    public void refresh(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String[] color = cube.getCube_up();
                up1.setBackgroundColor(Color.parseColor(color[0]));
                up2.setBackgroundColor(Color.parseColor(color[1]));
                up3.setBackgroundColor(Color.parseColor(color[2]));
                up4.setBackgroundColor(Color.parseColor(color[3]));
                color = cube.getCube_down();
                down1.setBackgroundColor(Color.parseColor(color[0]));
                down2.setBackgroundColor(Color.parseColor(color[1]));
                down3.setBackgroundColor(Color.parseColor(color[2]));
                down4.setBackgroundColor(Color.parseColor(color[3]));
                color = cube.getCube_front();
                front1.setBackgroundColor(Color.parseColor(color[0]));
                front2.setBackgroundColor(Color.parseColor(color[1]));
                front3.setBackgroundColor(Color.parseColor(color[2]));
                front4.setBackgroundColor(Color.parseColor(color[3]));
                color = cube.getCube_back();
                back1.setBackgroundColor(Color.parseColor(color[0]));
                back2.setBackgroundColor(Color.parseColor(color[1]));
                back3.setBackgroundColor(Color.parseColor(color[2]));
                back4.setBackgroundColor(Color.parseColor(color[3]));
                color = cube.getCube_left();
                left1.setBackgroundColor(Color.parseColor(color[0]));
                left2.setBackgroundColor(Color.parseColor(color[1]));
                left3.setBackgroundColor(Color.parseColor(color[2]));
                left4.setBackgroundColor(Color.parseColor(color[3]));
                color = cube.getCube_right();
                right1.setBackgroundColor(Color.parseColor(color[0]));
                right2.setBackgroundColor(Color.parseColor(color[1]));
                right3.setBackgroundColor(Color.parseColor(color[2]));
                right4.setBackgroundColor(Color.parseColor(color[3]));

                int[] state = cube.get_state();
                if(state[0]==1){up.setTextColor(Color.RED);}
                else{up.setTextColor(Color.WHITE);}
                if(state[1]==1){front.setTextColor(Color.RED);}
                else{front.setTextColor(Color.WHITE);}
                if(state[2]==1){left.setTextColor(Color.RED);}
                else{left.setTextColor(Color.WHITE);}
                if(state[3]==1){down.setTextColor(Color.RED);}
                else{down.setTextColor(Color.WHITE);}
                if(state[4]==1){back.setTextColor(Color.RED);}
                else{back.setTextColor(Color.WHITE);}
                if(state[5]==1){right.setTextColor(Color.RED);}
                else{right.setTextColor(Color.WHITE);}
            }
        });
    }
    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
              Camera.Size previewSize = camera.getParameters().getPreviewSize();
              previewHeight = previewSize.height;
              previewWidth = previewSize.width;
              rgbBytes = new int[previewWidth * previewHeight];
              onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        lastPreviewFrame = bytes;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter = new Runnable() {
            @Override
            public void run() {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
            }
        };

        postInferenceCallback = new Runnable() {
            @Override
            public void run() {
              camera.addCallbackBuffer(bytes);
              isProcessingFrame = false;
            }
        };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
      //We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }

            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                new Runnable() {
                  @Override
                  public void run() {
                    ImageUtils.convertYUV420ToARGB8888(
                        yuvBytes[0],
                        yuvBytes[1],
                        yuvBytes[2],
                        previewWidth,
                        previewHeight,
                        yRowStride,
                        uvRowStride,
                        uvPixelStride,
                        rgbBytes);
                  }
                };

            postInferenceCallback =
                new Runnable() {
                  @Override
                  public void run() {
                    image.close();
                    isProcessingFrame = false;
                  }
                };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
      Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        if (!isFinishing()) {
            LOGGER.d("Requesting finish");
            finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
        final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(CameraActivity.this,
              "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    /*
    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
        CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }
    */

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;

    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;

        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        new CameraConnectionFragment.ConnectionCallback() {
                            @Override
                            public void onPreviewSizeChosen(final Size size, final int rotation) {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                CameraActivity.this.onPreviewSizeChosen(size, rotation);
                            }
                        },
                        this,
                        getLayoutId(),
                        getDesiredPreviewFrameSize());

        camera2Fragment.setCamera(cameraId);
        fragment = camera2Fragment;

        getFragmentManager()
          .beginTransaction()
          .replace(R.id.container, fragment)
          .commit();

    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
      // Because of the variable row stride it's not possible to know in
      // advance the actual necessary dimensions of the yuv planes.
      for (int i = 0; i < planes.length; ++i) {
        final ByteBuffer buffer = planes[i].getBuffer();
        if (yuvBytes[i] == null) {
          LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
          yuvBytes[i] = new byte[buffer.capacity()];
        }
        buffer.get(yuvBytes[i]);
      }
    }

    public boolean isDebug() {
      return debug;
    }

    public void requestRender() {
      final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
      if (overlay != null) {
        overlay.postInvalidate();
      }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
      final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
      if (overlay != null) {
        overlay.addCallback(callback);
      }
    }

    public void onSetDebug(final boolean debug) {}

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        debug = !debug;
        requestRender();
        onSetDebug(debug);
        return true;
      }
      return super.onKeyDown(keyCode, event);
    }

    protected void readyForNextImage() {
      if (postInferenceCallback != null) {
        postInferenceCallback.run();
      }
    }

    protected int getScreenOrientation() {
      switch (getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_270:
          return 270;
        case Surface.ROTATION_180:
          return 180;
        case Surface.ROTATION_90:
          return 90;
        default:
          return 0;
      }
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract int getLayoutId();
    protected abstract Size getDesiredPreviewFrameSize();
}
