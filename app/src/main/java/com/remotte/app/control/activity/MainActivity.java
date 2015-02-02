package com.remotte.app.control.activity;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.remotte.app.control.ConnectService;
import com.remotte.app.control.activity.CardDemoActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

public class MainActivity extends Activity {

    public static final String TAG = "Remotte";

    private CardBuilder cardText;
    private CardScrollView mCardScroller;
    private View mView;
    private RemotteConnectReceiver receiver;
    private boolean forced = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        receiver = new RemotteConnectReceiver();

        mView = buildView("Tap to start");

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);

                mView = buildView("Scanning...");
                mCardScroller.getAdapter().notifyDataSetChanged();
                startService(new Intent(getApplicationContext(), ConnectService.class));
            }
        });

        setContentView(mCardScroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();

        if(receiver == null) receiver = new RemotteConnectReceiver();
        IntentFilter intentFilter = new IntentFilter("com.remotte.REMOTTE_CONNECTED");
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    protected void onDestroy() {
        if(receiver != null) unregisterReceiver(receiver);
        if(!forced) stopService(new Intent(getBaseContext(), ConnectService.class));
        super.onDestroy();
    }

    private View buildView(String text) {
        if(cardText == null) {
            cardText = new CardBuilder(this, CardBuilder.Layout.TEXT);
        }

        cardText.setText(text);
        return cardText.getView();
    }

    private class RemotteConnectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.remotte.REMOTTE_CONNECTED")) {
                Log.d(TAG, "Initialize Remotte demo");
                startActivity(new Intent(getBaseContext(), CardDemoActivity.class));
                forced = true;
                finish();
            }
        }
    }
}
