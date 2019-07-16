package com.example.directchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    Button btnOnOff, btnDiscover,btnSend;
    ListView listView;
    TextView read_msg_box, ConnectionStatus;
    EditText write_msg;

    WifiManager wifiManager;


    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ=1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }


    Handler handler =new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBuff=(byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    read_msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("Switch On Wifi");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("Switch Off Wifi");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        ConnectionStatus.setText("Discovering Nearby Devices");
                    }

                    @Override
                    public void onFailure(int i) {
                        ConnectionStatus.setText("Failed to discover devices ");

                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device= deviceArray[i];
                WifiP2pConfig config= new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to"+device.deviceName,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg=write_msg.getText().toString();
                // sendReceive.write(msg.getBytes());

                sendtask t1=new sendtask(msg);
                t1.execute();


            }
        });
    }


    public class sendtask extends AsyncTask<Void, Void, Void> {

        String message;


        sendtask(String msg) {
            message=msg;

        }

        @Override
        protected Void doInBackground(Void... arg0) {

            sendReceive.write(message.getBytes());

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            super.onPostExecute(result);

        }

    }
    private void initialWork() {
        btnOnOff=(Button) findViewById(R.id.WifiToggle);
        btnDiscover=(Button) findViewById(R.id.Discover);
        btnSend=(Button) findViewById(R.id.Send);
        listView=(ListView)findViewById(R.id.peerListView);
        read_msg_box=(TextView)findViewById(R.id.ReadMsg);
        ConnectionStatus=(TextView)findViewById(R.id.ConnectionStatus);
        write_msg= (EditText) findViewById(R.id.SendMsg);
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        mManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel= mManager.initialize(this,getMainLooper(),null);


        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChannel,this);
        mIntentFilter= new IntentFilter();

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index=0;

                for(WifiP2pDevice device : peerList.getDeviceList()){
                     deviceNameArray[index]= device.deviceName;
                     deviceArray[index]= device;
                     index++;
                }

                ArrayAdapter<String> adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);

                listView.setAdapter(adapter);
            }

            if(peers.size()==0){

                Toast.makeText(getApplicationContext(),"No nearby device found",Toast.LENGTH_LONG).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

            final InetAddress groupOwnerAddress= wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                ConnectionStatus.setText("Host");
                serverClass=new ServerClass();
                serverClass.start();
            }else if(wifiP2pInfo.groupFormed){
                ConnectionStatus.setText("Client");
                clientClass=new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends  Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                //ServerSocket s = new ServerSocket(0);
                //int port= s.getLocalPort();
                serverSocket=new ServerSocket(8888);
                socket=serverSocket.accept();
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class SendReceive extends Thread{
        private  Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while(socket!=null){
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void write(final byte[] bytes){


                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

        }

    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;



        public ClientClass(InetAddress hostAddress){
            hostAdd=hostAddress.getHostAddress();
            socket=new Socket();

        }

        @Override
        public void run() {
            try {
                //ServerSocket s = new ServerSocket(0);
                //int port= s.getLocalPort();
                socket.connect(new InetSocketAddress(hostAdd, 8888),500);
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
