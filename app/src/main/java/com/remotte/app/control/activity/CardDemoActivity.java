package com.remotte.app.control.activity;

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

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.remotte.app.control.ConnectService;
import com.remotte.app.control.R;

import java.util.ArrayList;

/**
 * Created by Oscar on 1/31/2015.
 */

public class CardDemoActivity extends Activity {

    private CardScrollView mCardScroller;
    private ArrayList<CardBuilder> mlcCards = new ArrayList<>();
    private ServiceReceiver receiver;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        receiver = new ServiceReceiver();

        buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mlcCards.size();
            }

            @Override
            public Object getItem(int position) {
                return mlcCards.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mlcCards.get(position).getView();
            }

            @Override
            public int getPosition(Object item) {
                return 0;
            }
        });
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
            }
        });

        setContentView(mCardScroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();

        if(receiver == null) receiver = new ServiceReceiver();
        IntentFilter intentFilter = new IntentFilter("com.remotte.REMOTTE_DISCONNECTED");
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver != null) unregisterReceiver(receiver);
        stopService(new Intent(this, ConnectService.class));
    }

    private void buildView() {
        CardBuilder card1 = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card1.setText("Swipe right (3)");
        card1.addImage(R.drawable.cat1);
        mlcCards.add(card1);

        CardBuilder card2 = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card2.setText("Swipe right (2)");
        card2.addImage(R.drawable.cat2);
        mlcCards.add(card2);

        CardBuilder card3 = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card3.setText("Swipe right (1)");
        card3.addImage(R.drawable.cat3);
        mlcCards.add(card3);

        CardBuilder card4 = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card4.setText("Done, click to continue");
        card4.addImage(R.drawable.cat4);
        mlcCards.add(card4);
    }

    private class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.remotte.REMOTTE_DISCONNECTED")) {
                Log.d(MainActivity.TAG, "Lost Remotte connection, initialize mainActivity");
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                finish();
            }
        }
    }

}
