package com.example.juan.juanwalkie;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

    public class PublicRoom extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private MediaRecorder mRecorder;
    private String mFileName;

    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_room);

        try {
            IO.Options opts = new IO.Options();
            opts.query = "name=" + getIntent().getStringExtra("NAME");
            mSocket = IO.socket("http://juanwalkie.herokuapp.com", opts);
        } catch (URISyntaxException e) {
            Log.w("ERROR CONNECT SOCKET", "onCreate: mSocket", e);
        }

        setTitle("Public room");

        mFileName = getCacheDir().getAbsolutePath();
        mFileName += "/juanwalkie.3gp";

        mSocket.connect();

        final TextView text_status = (TextView) findViewById(R.id.text_status);
        final TextView text_users = (TextView) findViewById(R.id.text_users);
        final TextView text_log = (TextView) findViewById(R.id.text_log);

        final View circle_status = findViewById(R.id.circle_status);
        final GradientDrawable color_status = (GradientDrawable)circle_status.getBackground();

        mSocket.on("USERS", new Emitter.Listener() {
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
                        text_users.setText("Connected users: " + total_users);
                        text_status.setText("Online");
                        color_status.setColor(getResources().getColor(R.color.online));
//                        // add the message to view
//                        addMessage(username, message);
                    }
                });
            }
        });

        mSocket.on("NEW_AUDIO", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        String user_name;
                        String audioBase64;
                        byte[] decodedBytes;
                        try {
                            audioBase64 = data.getString("audio");
                            Log.i("NEW_AUDIO", audioBase64);
                            user_name = data.getString("name");
                            decodedBytes = Base64.decode(audioBase64, 0);
                            try {
                                writeToFile(decodedBytes, mFileName);
                                text_log.setText(user_name + " are talking");
                                startPlaying();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            Log.w("ERROR", "run: getAUDIO", e);
                            return;
                        }
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

        final View record_audio = findViewById(R.id.record_audio);
        final GradientDrawable bgShape = (GradientDrawable)record_audio.getBackground();
        record_audio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        bgShape.setColor(getResources().getColor(R.color.colorRecord));
                        startRecording();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stopRecording();
                        bgShape.setColor(getResources().getColor(R.color.colorPrimaryDark));
                        sendAudio();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("Media recoder", "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("PLAYING RECORD", "prepare() failed");
        }
    }

    private void sendAudio (){
        File file = new File(mFileName);
        try {
            byte[] FileBytes = FileUtils.readFileToByteArray(file);
            byte[] encodedBytes = Base64.encode(FileBytes, 0);
            String encodedString = new String(encodedBytes);
            mSocket.emit("USER_AUDIO", encodedString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(byte[] data, String fileName) throws IOException{
        FileOutputStream out = new FileOutputStream(fileName);
        out.write(data);
        out.close();
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
