package com.cs296.kainrath.cs296project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroup;

import java.util.List;

public class ChatGroupActivity extends AppCompatActivity {
    private static String TAG = "ChatGroupActivity";

    private ChatMessageAdaptor messageAdaptor;
    public List<Pair<String, String>> chatLog;
    private int chatId;
    private String email;

    private ListView messageList;
    private EditText messageToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_group);

        Intent intent = this.getIntent();
        chatId = intent.getIntExtra("ChatId", -1);
        if (chatId == -1) {
            Toast.makeText(this, "Failed to open Chat Group", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
        }
        email = GlobalVars.getUser().getEmail();

        messageList = (ListView) findViewById(R.id.chat_message_list);
        messageToSend = (EditText) findViewById(R.id.message_to_send);
        //sendButton = (Button) findViewById(R.id.message_send_button);

        chatLog = GlobalVars.getChatMessageLog(chatId);
        messageAdaptor = new ChatMessageAdaptor(this, R.layout.other_user_message_layout, chatLog, email);
        messageList.setAdapter(messageAdaptor);
    }

    public void onSendMessageClick(View view) {
        Log.d(TAG, "Sending a message");
        String message = messageToSend.getText().toString();
        if (message != null && !message.isEmpty()) {
            new AsyncSendMessage(email, chatId, message).execute();
            messageToSend.setText("");
        } else {
            Log.d(TAG, "Empty message, not sending");
        }
    }

    // THE FOLLOWING IS FOR RECEIVING NEW MESSAGE BROADCASTS
    @Override
    protected void onPause() {
        super.onPause();
        // Stop Broadcast receiver
        this.unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start broadcast receiver
        this.registerReceiver(messageReceiver, new IntentFilter("MessageUpdate"));
    }

    // Received new GCM message
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        private String TAG = "ChatGroupBroadCastRec";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Recieved new message broadcast");
            final int chatIdIncoming = intent.getIntExtra("ChatId", -1);
            if (chatIdIncoming == chatId) { // Update UI if the message was for this chatGroup, else ignore
                // Updating UI must be done on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (chatId == chatIdIncoming) {
                            Log.d(TAG, "Running on UI thread");
                            messageAdaptor.notifyDataSetChanged();

                        }
                    }
                });
            } else {
                Log.d(TAG, "new message was for a different chat");
            }
        }
    };

}

class ChatMessageAdaptor extends ArrayAdapter<Pair<String, String>> {
    private static String TAG = "ChatMessageAdaptor";
    List<Pair<String, String>> chatLog;
    private String email;

    private static LayoutInflater inflater;

    public ChatMessageAdaptor(Context context, int resId, List<Pair<String, String>> chatLog, String email) {
        super(context, resId, chatLog);
        this.chatLog = chatLog;
        this.email = email;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatLog.get(position).first.equals(email)) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int getCount() {
        Log.d(TAG, "Getting count, " + chatLog.size());
        return chatLog.size();
    }

    @Override
    public Pair<String, String> getItem(int position) {
        Log.d(TAG, "Getting chatGroup at position " + position);
        return chatLog.get(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "Getting item id at position " + position);
        return position;
    }

    @Override
    public View getView(int position, View rowView, ViewGroup parent) {
        Log.d(TAG, "Updating list view position " + position);
        Pair<String, String> chatMsg = chatLog.get(position);

        if (rowView == null) {
            if (chatMsg.first.equals(email)) {
                rowView = inflater.inflate(R.layout.this_user_message_layout, null);
            } else {
                rowView = inflater.inflate(R.layout.other_user_message_layout, null);
            }
        }

        TextView sender;
        TextView msg;
        if (chatMsg.first.equals(email)) { // Message from this user
            sender = (TextView) rowView.findViewById(R.id.this_user_name);
            sender.setText(email);
            msg = (TextView) rowView.findViewById(R.id.this_user_msg);
        } else { // Message from other user
            sender = (TextView) rowView.findViewById(R.id.other_user_name);
            sender.setText(chatMsg.first);
            msg = (TextView) rowView.findViewById(R.id.other_user_msg);
        }

        msg.setText(chatMsg.second);

        return rowView;
    }
}