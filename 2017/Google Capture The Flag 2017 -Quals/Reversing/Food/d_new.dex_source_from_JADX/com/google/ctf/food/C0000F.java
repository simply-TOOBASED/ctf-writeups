package com.google.ctf.food;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class C0000F extends BroadcastReceiver {
    private static byte[] flag = new byte[]{(byte) -19, (byte) 116, (byte) 58, (byte) 108, (byte) -1, (byte) 33, (byte) 9, (byte) 61, (byte) -61, (byte) -37, (byte) 108, (byte) -123, (byte) 3, (byte) 35, (byte) 97, (byte) -10, (byte) -15, (byte) 15, (byte) -85, (byte) -66, (byte) -31, (byte) -65, (byte) 17, (byte) 79, (byte) 31, (byte) 25, (byte) -39, (byte) 95, (byte) 93, (byte) 1, (byte) -110, (byte) -103, (byte) -118, (byte) -38, (byte) -57, (byte) -58, (byte) -51, (byte) -79};
    private Activity f0a;
    private int f1c;
    private byte[] f2k = new byte[8];

    public C0000F(Activity activity) {
        this.f0a = activity;
        for (int i = 0; i < 8; i++) {
            this.f2k[i] = (byte) 0;
        }
        this.f1c = 0;
    }

    public void onReceive(Context context, Intent intent) {
        this.f2k[this.f1c] = (byte) intent.getExtras().getInt("id");
        cc();
        this.f1c++;
        if (this.f1c == 8) {
            this.f1c = 0;
            this.f2k = new byte[8];
            for (int i = 0; i < 8; i++) {
                this.f2k[i] = (byte) 0;
            }
        }
    }

    public void cc() {
        byte[] bArr = new byte[]{(byte) 26, (byte) 27, (byte) 30, (byte) 4, (byte) 21, (byte) 2, (byte) 18, (byte) 7};
        for (int i = 0; i < 8; i++) {
            bArr[i] = (byte) (bArr[i] ^ this.f2k[i]);
        }
        if (new String(bArr).compareTo("\u0013\u0011\u0013\u0003\u0004\u0003\u0001\u0005") == 0) {
            Toast.makeText(this.f0a.getApplicationContext(), new String(C0004.m0(flag, this.f2k)), 1).show();
        }
    }
}
