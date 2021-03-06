package ca.ualberta.ishelf;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

public class ScanActivity extends AppCompatActivity {

    // camera variables
    private CameraManager cameraManager;
    private int cameraFacing;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession scanSession;
    private CaptureRequest captureRequest;
    final String TAG = "scanActivity";

    private Size Size;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private CameraDevice.StateCallback stateCallback;
    private TextureView.SurfaceTextureListener surfaceTextureListener;

    // strings
    private String cameraId;

    private Bitmap lastScan;
    private String outputISBN = "";

    FirebaseVisionBarcodeDetectorOptions options;
    FirebaseVisionBarcodeDetector detector;

    private int CAMERA_REQUEST_CODE = 29;

    // elements on screen
    private TextureView textureView;
    private FloatingActionButton scanFab;
    private Button lastISBN;
    private FrameLayout shutter;
    private final AlphaAnimation fade = new AlphaAnimation(1, 0);
    private ProgressBar progressBar;

    private String visit;


    String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private final OkHttpClient client = new OkHttpClient();
    private String description = "";
    private String title = "";
    private String year = "";
    private String genre = "";
    private String author = "";
    private String URLimage = "";
    private Button finishScanButton;

    private boolean testing = false;
    private boolean finished_computation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // need to use camera
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);

        // get elements on screen
        textureView = (TextureView) findViewById(R.id.texture_view);
        scanFab = (FloatingActionButton) findViewById(R.id.scan_fab);
        lastISBN = (Button) findViewById(R.id.last_ISBN);
        shutter = (FrameLayout) findViewById(R.id.snapshot_effect);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar_scan);


        Intent intent = getIntent();
        visit = intent.getStringExtra("task");

        finishScanButton = (Button) findViewById(R.id.accept_scan_button);
        FloatingActionButton backScan = (FloatingActionButton) findViewById(R.id.back_button_camera);

        finishScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finished_computation) {
                    Bundle extras = new Bundle();
                    extras.putString("ISBN", outputISBN);
                    extras.putString("description", description);
                    extras.putString("title", title);
                    extras.putString("year", year);
                    extras.putString("author", author);
                    extras.putString("genre", genre);
                    extras.putString("URL", URLimage);

                    if (visit.equals("get_description")) {
                        Intent intent = new Intent(ScanActivity.this, EditBookActivity.class);
                    } else if (visit.equals("lend")) {
                        Intent intent = new Intent(ScanActivity.this, BookProfileActivity.class);
                    }
                    intent.putExtras(extras);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        backScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();

            }
        });

        shutter.setVisibility(View.INVISIBLE);
        fade.setDuration(1000);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation anim) {
                shutter.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animation anim) {
            }
            @Override
            public void onAnimationStart(Animation anim) {
            }
        });

        // basic camera setup
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        // set up ISBN scanner
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_EAN_13)
                        .build();

        detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);

        scanFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finished_computation = false;
                shutter.startAnimation(fade);
                lastScan = textureView.getBitmap();
                //lastScan = BitmapFactory.decodeResource(getResources(), R.drawable.isbn_test2);
                if (!testing) {
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(lastScan);
                    Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionBarcode> barCodes) {
                                    if (barCodes.size() > 0) {
                                        FirebaseVisionBarcode ISBN = barCodes.get(0);
                                        outputISBN = ISBN.getRawValue();
                                        lastISBN.setText(outputISBN);
                                        progressBar.setVisibility(View.VISIBLE);
                                        getAsyncCall();
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                }
                            });
                }
                if (testing) {
                    outputISBN = "9780307401199";
                    lastISBN.setText(outputISBN);
                    progressBar.setVisibility(View.VISIBLE);
                    getAsyncCall();
                }
            }
        });

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                initializeCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };


        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                ScanActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                ScanActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                ScanActivity.this.cameraDevice = null;
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            initializeCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
        Log.d("cam","camStop");
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        closeCamera();
//        closeBackgroundThread();
//    }

    private void closeCamera() {
        if (scanSession != null) {
            scanSession.close();
            scanSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(Size.getWidth(), Size.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                ScanActivity.this.scanSession = cameraCaptureSession;
                                ScanActivity.this.scanSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.d("cam","cam3");
            e.printStackTrace();
        }
    }

    // sets up the camera
    private void initializeCamera() {
            try {
                for (String cameraId : cameraManager.getCameraIdList()) {
                    CameraCharacteristics cameraCharacteristics =
                            cameraManager.getCameraCharacteristics(cameraId);
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                            cameraFacing) {
                        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        Size = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                        this.cameraId = cameraId;
                    }
                }
            } catch (CameraAccessException e) {
                Log.d("cam","cam1");
                e.printStackTrace();
            }
    }

    // opens the camera
    private void openCamera() {
            try {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
                }
            } catch (CameraAccessException e) {
                Log.d("cam","cam2");
                e.printStackTrace();
            }
    }

    // opens background thread
    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void getAsyncCall(){
        Request request = new Request.Builder()
                .url(url + outputISBN)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "error in getting response using async okhttp call");
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                String string = responseBody.string();

                JSONObject urlJSON = null;
                try {
                    urlJSON = new JSONObject(string);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JSONArray books = null;
                try {
                    books = urlJSON.getJSONArray("items");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < books.length(); i++) {
                    JSONObject book = null;
                    try {
                        book = books.getJSONObject(i);
                    } catch (JSONException e) {
                        continue;
                    }
                    JSONObject volumeInfo = null;
                    try {
                        volumeInfo = book.getJSONObject("volumeInfo");
                    } catch (JSONException e) {
                        continue;
                    }
                    JSONArray authors = null;
                    try {
                        authors = volumeInfo.getJSONArray("authors");
                    } catch (JSONException e) {
                        continue;
                    }
                    JSONArray genres = null;
                    try {
                        genres = volumeInfo.getJSONArray("categories");
                    } catch (JSONException e) {
                        continue;
                    }
                    try {
                        genre = genres.getString(0);
                    } catch (JSONException e) {
                        continue;
                    }
                    try {
                        author = authors.getString(0);
                    } catch (JSONException e) {
                       continue;
                    }
                    try {
                        description = volumeInfo.getString("description");
                    } catch (JSONException e) {
                        continue;
                    }

                    try {
                        title = volumeInfo.getString("title");
                    } catch (JSONException e) {
                        continue;
                    }
                    try {
                        year = volumeInfo.getString("publishedDate");
                    } catch (JSONException e) {
                        continue;
                    }
                    JSONObject imageLinks;
                    try {
                        imageLinks = volumeInfo.getJSONObject("imageLinks");
                    } catch (JSONException e) {
                        continue;
                    }
                    try {
                        URLimage = imageLinks.getString("thumbnail");
                    } catch (JSONException e) {
                        continue;
                    }
                    fixImageURL(URLimage);
                }



                // Remember to set the bitmap in the main thread.
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (!lastISBN.equals("")) {
                            finishScanButton.setBackground(getDrawable(R.drawable.gradientbutton));
                        }
                        finished_computation = true;
                    }
                });

                if (!response.isSuccessful()) {
                    throw new IOException("Error response " + response);
                }

            }
        });
    }

    public void fixImageURL(String URL) {
        URLimage = URLimage.replace("http", "https");
        URLimage = URLimage.replace("zoom=1", "zoom=0");
    }

}
