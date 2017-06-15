package com.crimeorsublime.crimeorsublime;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MenuActivity extends AppCompatActivity {
    private static final String IMGUR_ID = "fa4129345194014";
    public ImageView mImageView;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mImageView = (ImageView) findViewById(R.id.image);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        new VerifySession().execute();
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
            new UploadImage().execute();
/*            if (data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap == null) {
                    Log.d("NOPE:", "Bitmap missing");
                    return;
                }

                Uri photoUri = (Uri) extras.get(MediaStore.EXTRA_OUTPUT);
                if (photoUri == null) {
                    Log.d("NOPE:", "Photo extra is missing");
                    return;
                }

                Log.d("YEP:", "You win!");

                mImageView.setImageBitmap(imageBitmap);
                new UploadImage().execute(imageBitmap);
            } else {
                Log.e("NOPE", "No way dood");
            } */
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

    private class UploadImage extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected String doInBackground(Bitmap... params) {
            //Bitmap imageBitmap = params[0];

            // String loginUrl = "http://192.168.0.164:8000/session-create-user";
            String imgurUrl = "https://api.imgur.com/3/image.json";
            BufferedReader reader;
            StringBuilder stringBuilder;
            InputStreamReader responseStream;
            ByteArrayOutputStream imageByteArrayOutputStream;
            byte[] imageBinary;
            String imageBase64Encoding;
            URL url;
            DataOutputStream bodyStream;
            String responseData;
            JSONObject response;
            JSONObject submission = new JSONObject();
            String stringResponse = null;

            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);

            Log.d("THE FILE:", mCurrentPhotoPath);

            if (imageBitmap == null) {
                Log.d("OMFG", "IT'S NULL");
            } else {
                Log.d("OMFG", "IT'S NOT NULL!!!");
            }

            try {

                url = new URL(imgurUrl);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "application/json");
                conn.setRequestProperty("Authorization", "Client-ID " + IMGUR_ID);
                bodyStream = new DataOutputStream(conn.getOutputStream());
                imageByteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageByteArrayOutputStream);
                imageBinary = imageByteArrayOutputStream.toByteArray();
                imageBase64Encoding = Base64.encodeToString(imageBinary, Base64.DEFAULT);

                submission.put("image", imageBase64Encoding);
                bodyStream.write(submission.toString().getBytes());
                bodyStream.close();

                responseStream = new InputStreamReader(conn.getInputStream());
                reader = new BufferedReader(responseStream);
                stringBuilder = new StringBuilder();
                while ((responseData = reader.readLine()) != null) {
                    stringBuilder.append(responseData);
                }

                reader.close();
                responseStream.close();

                response = new JSONObject(stringBuilder.toString());

                stringResponse = response.toString();

                // Use this for debugging
                Context context = getApplicationContext();
                Log.d("ImgurResponse:", stringResponse);

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
