package com.example.juan.juanwalkie;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class PublicRoom extends AppCompatActivity {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.1.18:3000");
        } catch (URISyntaxException e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_room);

        mSocket.connect();

        final TextView text_status = (TextView) findViewById(R.id.text_status);

        JSONObject user_data= new JSONObject();
        try {
            user_data.put("ID", getIntent().getStringExtra("ID"));
            user_data.put("NAME", getIntent().getStringExtra("NAME"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("USER_DATA", user_data.toString());
        mSocket.on("CONNECTED", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        int total_users;
//                        String message;
                        try {
                            total_users = data.getInt("total_users");
//                            message = data.getString("message");
                        } catch (JSONException e) {
                            return;
                        }
                        Log.i("TOTAL_USERS", total_users + "");
                        text_status.setText("Connected users: " + total_users);
//                        // add the message to view
//                        addMessage(username, message);
                    }
                });
            }
        });

        findViewById(R.id.sign_out).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.disconnect();
                returnMainActivity();
            }
        });
    }

    private void returnMainActivity(){
        MainActivity.signOut();
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
//        mSocket.off("new message", onNewMessage);
    }

}
