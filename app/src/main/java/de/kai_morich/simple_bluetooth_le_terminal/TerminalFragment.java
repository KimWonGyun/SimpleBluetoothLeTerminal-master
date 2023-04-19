package de.kai_morich.simple_bluetooth_le_terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Array;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextView sendText2;
    private TextView sendText3;
    private TextView sendText4;
    private TextView sendText5;
    private TextView sendText6;
    private TextView sendText7;
    private TextView sendText8;
    private TextView sendText9;
    private TextView sendText10;
    private TextView sendText11;
    private TextView sendText12;

    private Spinner spinner;
    private Spinner spinner1;

    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);  //프레그먼트에서 메뉴 사용하기
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());        // 데이터가 들어오면 라인이 아래로 내려감

        sendText = view.findViewById(R.id.send_text1);                               // edit text data(send data)
        sendText2 = view.findViewById(R.id.send_text2);
        sendText3 = view.findViewById(R.id.send_text3);
        sendText4 = view.findViewById(R.id.send_text4);
        sendText5 = view.findViewById(R.id.send_text5);
        sendText6 = view.findViewById(R.id.send_text6);
        sendText7 = view.findViewById(R.id.data6);
        sendText8 = view.findViewById(R.id.data7);
        sendText9 = view.findViewById(R.id.data8);
        sendText10 = view.findViewById(R.id.data9);
        sendText11 = view.findViewById(R.id.data10);
        sendText12 = view.findViewById(R.id.data11);

        //sendText7 = view.findViewById(R.id.send_text7);

        hexWatcher = new TextUtil.HexWatcher(sendText);                              // 핵
        hexWatcher.enable(hexEnabled);                                               // 사
        sendText.addTextChangedListener(hexWatcher);                                 // 모
        sendText.setHint(hexEnabled ? "HEX mode" : "");                              // 드   신경쓰지 말것

        View sendBtn = view.findViewById(R.id.send_btn);
        View sendBtn2 = view.findViewById(R.id.send_btn2);   // 전송 데이터
        //Button sendBtn3 = view.findViewById(R.id.send_btn3);
       // Button sendBtn4 = view.findViewById(R.id.send_btn4);
        Button sendBtn5 = view.findViewById(R.id.send_btn5);
        spinner = view.findViewById(R.id.spchlist);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        break;

                    case 1:
                        StringBuilder strBuilder1 = new StringBuilder();
                        strBuilder1.append("$DI_CH,0,*");                      //READ
                        send(strBuilder1.toString());
                        Toast.makeText(adapterView.getContext(), "CLOCK", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        StringBuilder strBuilder2 = new StringBuilder();
                        strBuilder2.append("$DI_CH,1,*");                      //READ
                        send(strBuilder2.toString());
                        Toast.makeText(adapterView.getContext(), "CLOCK + DAY OF THE WEEK", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        StringBuilder strBuilder3 = new StringBuilder();
                        strBuilder3.append("$DI_CH,2,*");                      //READ
                        send(strBuilder3.toString());
                        Toast.makeText(adapterView.getContext(), "CLOCK + TEMP + HUMIDITY", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        StringBuilder strBuilder4 = new StringBuilder();
                        strBuilder4.append("$DI_CH,3,*");                      //READ
                        send(strBuilder4.toString());
                        Toast.makeText(adapterView.getContext(), "CLOCK + (specific)TEMP + HUMIDITY", Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        StringBuilder strBuilder5 = new StringBuilder();
                        strBuilder5.append("$DI_CH,4,*");                      //READ
                        send(strBuilder5.toString());
                        Toast.makeText(adapterView.getContext(), "TEMP", Toast.LENGTH_SHORT).show();
                        break;
                    case 6:
                        StringBuilder strBuilder6 = new StringBuilder();
                        strBuilder6.append("$DI_CH,5,*");                      //READ
                        send(strBuilder6.toString());
                        Toast.makeText(adapterView.getContext(), "HUMIDITY", Toast.LENGTH_SHORT).show();
                        break;
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinner1= view.findViewById(R.id.DIM);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0:
                        break;

                    case 1:
                        StringBuilder strBuilder1 = new StringBuilder();
                        strBuilder1.append("$DIDIM,0,*");                      //READ
                        send(strBuilder1.toString());
                        Toast.makeText(adapterView.getContext(), "MAX", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        StringBuilder strBuilder2 = new StringBuilder();
                        strBuilder2.append("$DIDIM,1,*");                      //READ
                        send(strBuilder2.toString());
                        Toast.makeText(adapterView.getContext(), "5", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        StringBuilder strBuilder3 = new StringBuilder();
                        strBuilder3.append("$DIDIM,2,*");                      //READ
                        send(strBuilder3.toString());
                        Toast.makeText(adapterView.getContext(), "4", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        StringBuilder strBuilder4 = new StringBuilder();
                        strBuilder4.append("$DIDIM,3,*");                      //READ
                        send(strBuilder4.toString());
                        Toast.makeText(adapterView.getContext(), "3", Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        StringBuilder strBuilder5 = new StringBuilder();
                        strBuilder5.append("$DIDIM,4,*");                      //READ
                        send(strBuilder5.toString());
                        Toast.makeText(adapterView.getContext(), "2", Toast.LENGTH_SHORT).show();
                        break;
                    case 6:
                        StringBuilder strBuilder6 = new StringBuilder();
                        strBuilder6.append("$DIDIM,5,*");                      //READ
                        send(strBuilder6.toString());
                        Toast.makeText(adapterView.getContext(), "1", Toast.LENGTH_SHORT).show();
                        break;
                    case 7:
                        StringBuilder strBuilder7 = new StringBuilder();
                        strBuilder7.append("$DIDIM,6,*");                      //READ
                        send(strBuilder7.toString());
                        Toast.makeText(adapterView.getContext(), "LOW", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        //sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view1){
                StringBuilder strBuilder = new StringBuilder();    // BUFFER
                strBuilder.append("$DI_WR,");//append 데이터 붙이기
                strBuilder.append(sendText.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText2.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText3.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText4.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText5.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText7.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText8.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText9.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText10.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText11.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText12.getText().toString());
                strBuilder.append(",");
                strBuilder.append(sendText6.getText().toString());
                strBuilder.append(",");
                strBuilder.append("*");
                strBuilder.append("\r");
                strBuilder.append("\n");

                send(strBuilder.toString());                 // SEND
            }
        });

        sendBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//read
                StringBuilder strBuilder1 = new StringBuilder();
                strBuilder1.append("$DI_RD,0*");                      //READ
                send(strBuilder1.toString());
            }
        });

        sendBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//read
                StringBuilder strBuilder1 = new StringBuilder();
                strBuilder1.append("$DIRST,,*");                      //READ
                send(strBuilder1.toString());
            }
        });

        /*sendBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//read
                StringBuilder strBuilder1 = new StringBuilder();
                strBuilder1.append("$DI_CH,0,*");                      //READ
                send(strBuilder1.toString());
            }
        });
        */
        /*sendBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//read
                StringBuilder strBuilder1 = new StringBuilder();
                strBuilder1.append("$DI_CH,1,*");                      //READ
                send(strBuilder1.toString());
            }
        });
*/


        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        }
            else if (id == R.id.devnum){

                return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting to Daeyang IP clock");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }
    int ww = 1;
    private void receive(byte[] data) {
        if(hexEnabled) {
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
        /*
            String msg = new String(data);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        */
            StringBuilder sb  = new StringBuilder();
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();

            for (byte b : data)
            {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));//data 16진수로 받아서 String sb에 저장
            }
            sb.toString();                                                                            //16진수 BUFFER에 저장



            StringBuilder output = new StringBuilder();

            String str1 = null;
            for(int j = 0; j < sb.length(); j+=2) //2개씩 데이터 쪼개기
            {
                String str = sb.substring(j,j+2);
                int dec = Integer.parseInt(str,16); //10진수에도 저장
                sb1.append(dec); // 10진수 버퍼

                ww++;

                if(ww <9)
                {
                    output.append((char) Integer.parseInt(str, 16));
                    if(dec==75)
                    {
                        ww=1;
                        output.append("*"); // WRT_OK* 마지막 별 부분
                        break;
                    }
                }

                else if(ww>8 && ww<29)
                {
                    if(ww==9)
                    {
                        output.append("SERVER IP : ");

                    }

                    else if(ww==13)
                    {
                        output.append("\n");
                        output.append("LOCAL IP : ");
                    }

                    else if(ww==17)
                    {
                        output.append("\n");
                        output.append("GATE WAY : ");
                    }

                    else if(ww==21)
                    {
                        output.append("\n");
                        output.append("DNS : ");
                    }

                    else if(ww==25)
                    {
                        output.append("\n");
                        output.append("SUBNET : ");
                    }

                    output.append(dec);
                    output.append(".");

                    if(ww==28)
                    {
                        output.append("\n");
                        output.append("MAC : ");
                    }
                }

                else if(ww>28&ww<35)
                {
                    output.append(str);
                    output.append(".");// MAC 출력
                }

                else if(ww==35)//*****************************************************************************포트번호 다시 출력하기
                {
                    output.append("\n");
                    output.append("PORT : ");
                    sb2.append(str);
                    //output.append(dec);//16 진수로 출력해서 포트번호 확인하기
                }

                else  if(ww==36)
                {
                    sb2.append(str);
                    str1= sb2.toString();
                    int decimal = Integer.parseInt(str1,16);
                    output.append(decimal);
                }

                else if(ww==37)
                {
                    output.append((char) Integer.parseInt(str, 16));
                }
            }

            //receiveText.append(sb1);
            receiveText.append(output.toString());                //*******************************String으로 출력

            if(ww==37)
            {
                ww=1;
            }
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
