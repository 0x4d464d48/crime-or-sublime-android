package com.crimeorsublime.crimeorsublime;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1);
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
