package com.crimeorsublime.crimeorsublime;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void sendCredentials(View view) {
        final EditText emailInpputField = (EditText) findViewById(R.id.emailInput);
        final EditText passwordInputField = (EditText) findViewById(R.id.passwordInput);

        new SendLoginRequest().execute(emailInpputField.getText().toString(),
                passwordInputField.getText().toString());
    }

    private class SendLoginRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String password = params[1];

            String loginUrl = "http://192.168.0.164:8000/session-create-user";
            BufferedReader reader;
            StringBuilder stringBuilder;
            InputStreamReader responseStream;
            URL url;
            DataOutputStream bodyStream;
            String responseData;
            JSONObject response = null;
            JSONObject submission = new JSONObject();
            String stringResponse = null;

            try {
                submission.put("identifier", email);
                submission.put("password", password);

                url = new URL(loginUrl);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "application/json");
                bodyStream = new DataOutputStream( conn.getOutputStream() );

                bodyStream.write(submission.toString().getBytes());
                bodyStream.close();


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
                Log.d("NoTag:", stringResponse);

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

        @Override
        protected void onPostExecute(String result) {
            Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
            toast.show();
        }


    }

}
