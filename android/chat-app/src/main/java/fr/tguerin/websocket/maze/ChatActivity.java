package fr.tguerin.websocket.maze;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import fr.tguerin.websocket.maze.event.MessageReceivedEvent;
import fr.tguerin.websocket.maze.event.SendMessageEvent;
import fr.tguerin.websocket.maze.service.WebSocketClient;
import timber.log.Timber;


public class ChatActivity extends ActionBarActivity {

    private static final String STATE_MESSAGES = "stateMessages";

    @InjectView(R.id.message) EditText messageEditText;
    @InjectView(R.id.messages) ListView messagesListView;

    ArrayList<String> messages;

    ArrayAdapter<String> messagesAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.plant(new Timber.DebugTree());
        setContentView(R.layout.activity_maze);
        ButterKnife.inject(this);
        startService(new Intent(this, WebSocketClient.class));
        if (savedInstanceState != null) {
            messages = savedInstanceState.getStringArrayList(STATE_MESSAGES);
        } else {
            messages = new ArrayList<>();
        }
        messagesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messagesListView.setAdapter(messagesAdapter);
    }

    @Override protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @OnClick(R.id.send_message) public void onSendMessageCliked() {
        EventBus.getDefault().post(new SendMessageEvent(messageEditText.getText().toString()));
        messageEditText.setText("");
        messageEditText.clearFocus();
        hideKeyboard();
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);
    }

    public void onEventMainThread(MessageReceivedEvent messageReceivedEvent) {
        messages.add(messageReceivedEvent.message);
        messagesAdapter.notifyDataSetChanged();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_MESSAGES, messages);
    }

    @Override protected void onStop() {
        EventBus.getDefault().unregister(this);
        stopService(new Intent(this, WebSocketClient.class));
        super.onStop();
    }

}