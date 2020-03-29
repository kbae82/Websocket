package appforcivil.websocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class MainActivity extends AppCompatActivity {
    private OkHttpClient client;
    private WebSocket ws;
    protected static final float MAX_BUG_SPEED_DP_PER_S = 100f;
    String currentDirection = "stop";
    boolean leftControllerUp = true;
    boolean rightControllerUp = true;
    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences sharedpreferences;
    boolean doubleBackToExitPressedOnce = false;

    Button startAndStop;
    BugView bugView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Shared Preferences for IP address
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        setUIs();
    }

    private void setUIs(){
        //Toggle Button for Start and stop
        startAndStop = findViewById(R.id.start);
        startAndStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startAndStop.getText().toString().equalsIgnoreCase("START")){
                    start();
                }
                else{
                    stop();
                }
            }
        });

        Button btnIPSetting = findViewById(R.id.btn_ip_setting);
        btnIPSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupForIP();
            }
        });

        setJoystick();
    }

    //Modified Bugstick (Original src: https://github.com/justasm/Bugstick)
    private void setJoystick(){
        bugView =  findViewById(R.id.bugview);
        Joystick joystick1 = findViewById(R.id.joystick_f_b);
        joystick1.setJoystickListener(new JoystickListener() {

            //Check other control is on
            @Override
            public void onDown() {
                leftControllerUp = false;
            }

            @Override
            public void onDrag(float degrees, float offset) {
                if(!rightControllerUp){
                    return;
                }

                String directionValue = currentDirection;

                if(degrees == 90.0f && offset == 1.0f){
                    directionValue="forward";
                }
                if(degrees == -90.00f && offset == 1.0f){
                    directionValue="backward";
                }

                joyStickMove(directionValue, degrees, offset);
            }

            @Override
            public void onUp() {
                leftControllerUp = true;
                joyStickStop();
            }
        });


        Joystick joystick2 =  findViewById(R.id.joystick_l_r);
        joystick2.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                rightControllerUp = false;
            }

            @Override
            public void onDrag(float degrees, float offset) {
                if(!leftControllerUp){
                    return;
                }

                String directionValue = currentDirection;

                if(degrees == 0.0f && offset == 1.0f){
                    directionValue="right";
                }
                if(degrees == -180.00f && offset == 1.0f){
                    directionValue="left";
                }

                joyStickMove(directionValue, degrees, offset);
            }

            @Override
            public void onUp() {
                rightControllerUp = true;
                joyStickStop();
            }
        });
    }

    private void joyStickStop(){
        bugView.setVelocity(0, 0);

        if(leftControllerUp && rightControllerUp && (!currentDirection.equalsIgnoreCase("stop"))) {
            JSONObject jObject = null;

            try {
                jObject = new JSONObject("{'action':'stop'}");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ws.send(jObject.toString());
            currentDirection = "stop";
        }
    }

    private void joyStickMove(String directionValue, float degrees, float offset){
        if(!currentDirection.equalsIgnoreCase(directionValue)) {
            JSONObject jObject = null;
            try {
                jObject = new JSONObject("{'action':'" + directionValue + "'}");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ws.send(jObject.toString());
            currentDirection = directionValue;
        }

        bugView.setVelocity(
                (float) Math.cos(degrees * Math.PI / 180f) * offset * MAX_BUG_SPEED_DP_PER_S,
                -(float) Math.sin(degrees * Math.PI / 180f) * offset * MAX_BUG_SPEED_DP_PER_S);
    }


    //Setting up Popup UI for IP setting
    private void showPopupForIP() {
        AlertDialog.Builder codeDialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.popup_prompt_ip,
                (ViewGroup) findViewById(R.id.popup_root));
        final EditText IPInput = layout
                .findViewById(R.id.etxtCode);

        codeDialogBuilder.setTitle("Please Enter IP for the server");
        codeDialogBuilder.setView(layout);
        SpannableString spanButtonPos = new SpannableString("Confirm");
        IPInput.setVisibility(View.VISIBLE);
        IPInput.setText(sharedpreferences.getString("IP","ex)127.0.0.1:1234"));
        SpannableString spanButtonNeg = new SpannableString("Cancel");
        codeDialogBuilder.setPositiveButton(spanButtonPos,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogOK, int which) {

                    }
                });

        codeDialogBuilder.setNegativeButton(spanButtonNeg,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogOK, int which) {
                        dialogOK.dismiss();
                    }
                });

        final AlertDialog codeDialog = codeDialogBuilder.create();
        codeDialog.show();
        codeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("IP",IPInput.getText().toString());
                editor.commit();
                popToast("Please press 'START' button to connect");
                codeDialog.dismiss();
            }
        });
    }

    //Start Websocket and update UI
    private void start() {
        String WSURL = "ws://" + sharedpreferences.getString("IP","ex)127.0.0.1/1234");
        Request request = new Request.Builder().url(WSURL).build();
        AndroidWebSocketListener listener = new AndroidWebSocketListener();
        client = new OkHttpClient();
        ws = client.newWebSocket(request, listener);
        startAndStop.setText("STOP");
        Joystick joystick1 = findViewById(R.id.joystick_f_b);
        Joystick joystick2 = findViewById(R.id.joystick_l_r);
        joystick1.setVisibility(View.VISIBLE);
        joystick2.setVisibility(View.VISIBLE);
    }

    //Stop Websocket and reset UI
    private void stop() {
        JSONObject jObject = null;
        try {
            jObject = new JSONObject("{'action':'stop'}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        client.dispatcher().executorService().shutdown();
        ws.close(1000,"User EXIT");
        startAndStop.setText("START");
        Joystick joystick1 = (Joystick) findViewById(R.id.joystick_f_b);
        Joystick joystick2 = (Joystick) findViewById(R.id.joystick_l_r);
        joystick1.setVisibility(View.INVISIBLE);
        joystick2.setVisibility(View.INVISIBLE);
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(txt.indexOf("Error") > -1){
                    stop();
                    popToast("ERROR!!! Can't connect to the server");
                }
            }
        });
    }

    private void popToast(String inputMessage) {
        Toast toast = Toast.makeText(MainActivity.this, inputMessage,
                Toast.LENGTH_SHORT);
        LinearLayout toastLayout = (LinearLayout) toast.getView();
        TextView toastTV = (TextView) toastLayout.getChildAt(0);
        toastTV.setTextColor(Color.CYAN);
        toast.show();
    }

    //Override back button event with double tap exit UI
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finish();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        popToast("Please Tap 'BACK' button again to exit");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;

            }
        }, 2000);
    }

    //Finish the app: shutdown client and close Websocket connection
    @Override
    public void finish() {
        if(client!=null && ws!=null) {
            client.dispatcher().executorService().shutdown();
            ws.close(1000, "User EXIT");
        }
        super.finish();
    }

    //Override WebSocket Listener
    private final class AndroidWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d("WebSocketListener", "Open : " + response.message());
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d("WebSocketListener", "Receiving : " + text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.d("WebSocketListener", "Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.d("WebSocketListener", "Close : " + code + " / " + reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            //On Failed, show error message and reset UIs
            output("Error : " + t.getMessage());
        }
    }
}