package com.crimeorsublime.crimeorsublime;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MenuActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener {
    private GoogleApiClient mGoogleApiClient;
    private static final String IMGUR_ID = "fa4129345194014";
    public ImageView mImageView;
    private String mCurrentPhotoPath;
    private double currentLongitude;
    private Location location;
    private double currentLatitude;
    private LocationManager locationManager;
    private String reCaptchaToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mImageView = (ImageView) findViewById(R.id.image);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(SafetyNet.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
        getLocation();
    }


    public void verifyLogin(View view) {
        new VerifySession().execute();
    }

    public void takePhoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
/*                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.crimeorsublime.crimeorsublime.fileprovider",
                        photoFile);*/
                Uri photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 0);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mImageView = (ImageView) findViewById(R.id.image);

        if (requestCode == 0 && resultCode == RESULT_OK) {
            SafetyNet.SafetyNetApi.verifyWithRecaptcha(mGoogleApiClient, "6LcmkiUUAAAAACYiFpomZ7nL5FZszd3Rm7ieCSi1")
                    .setResultCallback(
                            new ResultCallback<SafetyNetApi.RecaptchaTokenResult>() {
                                @Override
                                public void onResult(SafetyNetApi.RecaptchaTokenResult result) {
                                    Status status = result.getStatus();
                                    if ((status != null) && status.isSuccess()) {
                                        Log.d("CAPT WIN:", "WIN!");
                                        if (!result.getTokenResult().isEmpty()) {
                                            // User response token must be validated using the
                                            // reCAPTCHA site verify API.
                                            reCaptchaToken = result.getTokenResult().toString();
                                            Log.d("THE TOKEN", reCaptchaToken);
                                            new UploadImage().execute();

                                        }
                                    } else {
                                        Log.d("CAPT LOOSE:", "Error occured getting reCaptcha");
                                        return;
                                    }
                                }
                            });
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("YO CONNECT FAILED:", connectionResult.getErrorMessage());
    }

    @Override
    public void onConnected(Bundle bundle) {
//        Log.d("YO CONNECT WORKED:", bundle.toString());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("YO CONNECT SUSPENDED:", Integer.toString(i));
    }

    @Override
    public void onLocationChanged(Location location) {
        this.currentLatitude = location.getLatitude();
        this.currentLongitude = location.getLongitude();

        Log.d("LONGITUDE ", Double.toString(this.currentLongitude));
        Log.d("LATITUDE ", Double.toString(this.currentLatitude));

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void getLocation() {
        try {
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5,
                    10,
                    this
            );

            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private class UploadImage extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected String doInBackground(Bitmap... params) {
            //Bitmap imageBitmap = params[0];

            // String loginUrl = "http://192.168.0.164:8000/session-create-user";
            String imgurUrl = "https://api.imgur.com/3/image.json";
            String cosUrl = "http://crime-or-sublime.herokuapp.com/graffiti-submit-new-submission";
            //String cosUrl = "http://192.168.0.164:8000/graffiti-submit-new-submission";

            BufferedReader reader;
            StringBuilder stringBuilder;
            InputStreamReader responseStream;
            ByteArrayOutputStream imageByteArrayOutputStream;
            byte[] imageBinary;
            String imageBase64Encoding;
            URL url;
            DataOutputStream bodyStream;
            DataOutputStream cosBodyStream;
            String responseData;
            JSONObject imgurResponse;
            JSONObject imgurSubmission = new JSONObject();
            JSONObject cosResponse;
            JSONObject cosSubmission = new JSONObject();
            String stringResponse = null;

            String graffitiUrl;

            HttpURLConnection imgurConn;
            HttpURLConnection cosConn;

            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);

            try {

                url = new URL(imgurUrl);

                imgurConn = (HttpURLConnection) url.openConnection();
                imgurConn.setRequestMethod("POST");
                imgurConn.setRequestProperty("Content-type", "application/json");
                imgurConn.setRequestProperty("Authorization", "Client-ID " + IMGUR_ID);
                bodyStream = new DataOutputStream(imgurConn.getOutputStream());
                imageByteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageByteArrayOutputStream);
                imageBinary = imageByteArrayOutputStream.toByteArray();
                imageBase64Encoding = Base64.encodeToString(imageBinary, Base64.DEFAULT);

                imgurSubmission.put("image", imageBase64Encoding);
                bodyStream.write(imgurSubmission.toString().getBytes());
                bodyStream.close();

                responseStream = new InputStreamReader(imgurConn.getInputStream());
                reader = new BufferedReader(responseStream);
                stringBuilder = new StringBuilder();
                while ((responseData = reader.readLine()) != null) {
                    stringBuilder.append(responseData);
                }

                reader.close();
                responseStream.close();

                imgurResponse = new JSONObject(stringBuilder.toString());

                stringResponse = imgurResponse.toString();

                // Use this for debugging
                Log.d("ImgurLink:", imgurResponse.getJSONObject("data").getString("link"));

                graffitiUrl = new URL(imgurResponse.getJSONObject("data").getString("link")).getPath();
                Log.d("GraffitiUrl:", graffitiUrl);
                graffitiUrl = graffitiUrl.substring(1, graffitiUrl.indexOf('.'));
                Log.d("Graffiti ID: ", graffitiUrl);

                getLocation();
                Log.d("Latitude", Double.toString(location.getLatitude()));
                Log.d("Longitude", Double.toString(location.getLongitude()));

                // Upload to CoS
                url = new URL(cosUrl);
                cosConn = (HttpURLConnection) url.openConnection();
                cosConn.setRequestMethod("POST");
                cosConn.setRequestProperty("Content-type", "application/json");
                cosSubmission.put("id", graffitiUrl);
                cosSubmission.put("latitude", location.getLatitude());
                cosSubmission.put("longitude", location.getLongitude());
                cosSubmission.put("recaptcha", reCaptchaToken);
                cosBodyStream = new DataOutputStream(cosConn.getOutputStream());


                cosBodyStream.write(cosSubmission.toString().getBytes());
                cosBodyStream.close();

                stringBuilder = new StringBuilder();
                responseStream = new InputStreamReader(cosConn.getInputStream());
                reader = new BufferedReader(responseStream);

                while ((responseData = reader.readLine()) != null) {
                    stringBuilder.append(responseData);
                }

                Log.d("COS RESPONSE: ", stringBuilder.toString());



            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return stringResponse;

        }
    }


    private class VerifySession extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String verifySessionUrl = "http://crime-or-sublime.herokuapp.com/session-verify-user";
            BufferedReader reader;
            StringBuilder stringBuilder;
            InputStreamReader responseStream;
            URL url;
            String responseData;
            JSONObject response;
            String stringResponse = null;

            try {

                url = new URL(verifySessionUrl);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "application/json");

                responseStream = new InputStreamReader(conn.getInputStream());
                reader = new BufferedReader(responseStream);
                stringBuilder = new StringBuilder();

                while((responseData = reader.readLine()) != null) {
                    stringBuilder.append(responseData);
                }

                reader.close();
                responseStream.close();

                response = new JSONObject(stringBuilder.toString());

                stringResponse = response.toString();

                // Use this for debugging
                Context context = getApplicationContext();
                Log.d("Verify:", stringResponse);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return stringResponse;

        }
    }
}
