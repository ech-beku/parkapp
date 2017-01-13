package ch.ffhs.parkapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnSignalChangedListener {


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 23487;
    private int token;
    private int eTicketNumber;

    private boolean canGetTicket = true;
    private boolean canPayTicket = false;

    private BeaconManager beaconManager;

    private static final int EntryBeaconMinor = 1;
    private static final int OutBeaconMinor = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        token = new Random().nextInt();
        eTicketNumber = -1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });


                builder.show();
            }else{
                startListeningForbeacons();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TAG", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            startListeningForbeacons();
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startListeningForbeacons(){

        beaconManager = new BeaconManager();
        beaconManager.setOnSignalChangedListener(this);
        beaconManager.startScan("fda50693a4e24fb1afcfc6eb07647825");




    }


    public void getTicket(View view) {

        //TODO: Proceed calling Service here to get ticket
        //https://parking-rest-server.herokuapp.com/api/get_ticket?&token=3535636333

        Fuel.get("http://parking-rest-server.herokuapp.com/api/get_ticket?token="+String.valueOf(token)).responseString(new Handler<String>() {
            @Override
            public void failure(Request request, Response response, FuelError error) {
                //do something when it is failure
                Log.d("error", error.toString());
            }

            @Override
            public void success(Request request, Response response, String data) {
                //do something when it is successful

                try {
                    JSONObject ticket = new JSONObject(data);

                    findViewById(R.id.gotTicketPanel).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.ticketInfoLabel)).setText("Ticket gelöst; Eticket Nummer: " + ticket.get("eTicketNumber") + "\nVorgeschlagener Parkplatz: " + ticket.get("proposedParkingspace"));

                    findViewById(R.id.getTicketButton).setEnabled(false);


                    eTicketNumber = ticket.getInt("eTicketNumber");
                    canPayTicket = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void payAndGo(View view) {

        // https://parking-rest-server.herokuapp.com/api/pay_ticket?token=43525
pay();

    }
    private void pay() {

        Fuel.get("http://parking-rest-server.herokuapp.com/api/pay_ticket?token=" + String.valueOf(token), null).responseString(new Handler<String>() {
            @Override
            public void failure(Request request, Response response, FuelError error) {
                //do something when it is failure
                Log.d("error", error.toString());
            }

            @Override
            public void success(Request request, Response response, String data) {
                //do something when it is successful

                try {
                    JSONObject answer = new JSONObject(data);


                    openSchranke();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void openSchranke(){
        Fuel.get("http://parking-rest-server.herokuapp.com/api/open_gate?eTicketNumber=" + String.valueOf(eTicketNumber) + "&token=" + String.valueOf(token), null).responseString(new Handler<String>() {
            @Override
            public void failure(Request request, Response response, FuelError error) {
                //do something when it is failure
                Log.d("error", error.toString());
            }

            @Override
            public void success(Request request, Response response, String data) {
                //do something when it is successful

                try {
                    JSONObject answer = new JSONObject(data);

                    if(answer.getBoolean("gateOpen")){

                        findViewById(R.id.readyToGo).setVisibility(View.VISIBLE);
                        findViewById(R.id.gotTicketPanel).setVisibility(View.INVISIBLE);
                        findViewById(R.id.getTicketButton).setEnabled(true);
                        eTicketNumber = -1;
                        canGetTicket = true;

                    }



                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    public void onSignalChanged(String data) {

    }

    @Override
    public void onSignalChanged(ArrayList<BeaconSignal> signals) {

        Log.d("beacon", "got some signals");

        for(BeaconSignal sig : signals){
            if(sig.getMinor() == EntryBeaconMinor && sig.getRssi() > -30 && canGetTicket){
                canGetTicket = false;
                this.getTicket(null);
            }

            if(sig.getMinor() == OutBeaconMinor && sig.getRssi() > -30 && canPayTicket){
                canPayTicket = false;
                this.payAndGo(null);
            }
        }

    }
}

