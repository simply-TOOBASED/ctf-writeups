package com.google.ctf.food;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.GridLayout;

public class C0003S {
    public static String f3I = "FLAG_FACTORY";
    public static Activity f4a;

    public C0003S(final Activity activity) {
        int i = 0;
        f4a = activity;
        Context applicationContext = activity.getApplicationContext();
        GridLayout gridLayout = (GridLayout) activity.findViewById(C0001R.id.foodLayout);
        String[] strArr = new String[]{"🍕", "🍬", "🍞", "🍎", "🍅", "🍙", "🍝", "🍓", "🍈", "🍉", "🌰", "🍗", "🍤", "🍦", "🍇", "🍌", "🍣", "🍄", "🍊", "🍒", "🍠", "🍍", "🍆", "🍟", "🍔", "🍜", "🍩", "🍚", "🍨", "🌾", "🌽", "🍖"};
        while (i < 32) {
            View button = new Button(applicationContext);
            LayoutParams layoutParams = new GridLayout.LayoutParams();
            layoutParams.width = (int) TypedValue.applyDimension(1, 60.0f, activity.getResources().getDisplayMetrics());
            layoutParams.height = (int) TypedValue.applyDimension(1, 60.0f, activity.getResources().getDisplayMetrics());
            button.setLayoutParams(layoutParams);
            button.setText(strArr[i]);
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    view.playSoundEffect(0);
                    Intent intent = new Intent(C0003S.f3I);
                    intent.putExtra("id", i);
                    activity.sendBroadcast(intent);
                }
            });
            gridLayout.addView(button);
            i++;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(f3I);
        activity.registerReceiver(new C0000F(activity), intentFilter);
    }
}
