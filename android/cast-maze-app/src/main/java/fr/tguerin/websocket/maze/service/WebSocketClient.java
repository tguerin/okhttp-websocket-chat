package fr.tguerin.websocket.maze.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.ws.WebSocket;
import com.squareup.okhttp.internal.ws.WebSocketListener;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import fr.tguerin.websocket.maze.event.MessageReceivedEvent;
import fr.tguerin.websocket.maze.event.SendMessageEvent;
import okio.Buffer;
import timber.log.Timber;

public class WebSocketClient extends Service implements WebSocketListener {

    private static final int CONNECT_TO_WEB_SOCKET = 1;
    private static final int SEND_MESSAGE = 2;
    private static final int CLOSE_WEB_SOCKET = 3;
    private static final int DISCONNECT_LOOPER = 4;

    private static final String KEY_MESSAGE = "keyMessage";

    private Handler mServiceHandler;
    private Looper mServiceLooper;
    private WebSocket mWebSocket;
    private boolean mConnected;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT_TO_WEB_SOCKET:
                    connectToWebSocket();
                    break;
                case SEND_MESSAGE:
                    sendMessageThroughWebSocket(msg.getData().getString(KEY_MESSAGE));
                    break;
                case CLOSE_WEB_SOCKET:
                    closeWebSocket();
                    break;
                case DISCONNECT_LOOPER:
                    mServiceLooper.quit();
                    break;
            }
        }
    }

    private void sendMessageThroughWebSocket(String message) {
        if (!mConnected) {
            return;
        }
        try {
            mWebSocket.sendMessage(WebSocket.PayloadType.TEXT, new Buffer().write(message.getBytes()));
        } catch (IOException e) {
            Timber.d("Error sending message", e);
        }
    }

    private void connectToWebSocket() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("ws://192.168.0.25:5000")
                .build();
        mWebSocket = WebSocket.newWebSocket(okHttpClient, request);
        try {
            Response response = mWebSocket.connect(WebSocketClient.this);
            if (response.code() == 101) {
                mConnected = true;
            } else {
                Timber.d("Couldn't connect to WebSocket %s %s %s", response.code(), response.message(), response.body().string());
            }

        } catch (IOException e) {
            Timber.d("Couldn't connect to WebSocket", e);
        }
    }

    private void closeWebSocket() {
        if (!mConnected) {
            return;
        }
        try {
            mWebSocket.close(1000, "Goodbye, World!");
        } catch (IOException e) {
            Timber.d("Failed to close WebSocket", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("WebSocket service");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        mServiceHandler.sendEmptyMessage(CONNECT_TO_WEB_SOCKET);

        EventBus.getDefault().register(this);
    }

    public void onEvent(SendMessageEvent sendMessageEvent) {
        if (!mWebSocket.isClosed()) {
            Message message = Message.obtain();
            message.what = SEND_MESSAGE;
            Bundle data = new Bundle();
            data.putString(KEY_MESSAGE, sendMessageEvent.message);
            message.setData(data);
            mServiceHandler.sendMessage(message);
        }
    }


    @Override
    public void onDestroy() {
        mServiceHandler.sendEmptyMessage(CLOSE_WEB_SOCKET);
        mServiceHandler.sendEmptyMessage(DISCONNECT_LOOPER);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onMessage(okio.BufferedSource payload, WebSocket.PayloadType type) throws IOException {
        if (type == WebSocket.PayloadType.TEXT) {
            EventBus.getDefault().post(new MessageReceivedEvent(payload.readUtf8()));
            payload.close();
        }
    }

    @Override
    public void onClose(int code, String reason) {
        mConnected = false;
        Timber.d("Websocket is closed %s %s", code, reason);
    }

    @Override
    public void onFailure(IOException e) {
        mConnected = false;
        Timber.d("Websocket is closed", e);
    }
}
