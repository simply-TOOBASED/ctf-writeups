package com.google.ctf.food;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class C0000F extends BroadcastReceiver {
    private static byte[] flag = new byte[]{(byte) -19, (byte) 116, (byte) 58, (byte) 108, (byte) -1, (byte) 33, (byte) 9, (byte) 61, (byte) -61, (byte) -37, (byte) 108, (byte) -123, (byte) 3, (byte) 35, (byte) 97, (byte) -10, (byte) -15, (byte) 15, (byte) -85, (byte) -66, (byte) -31, (byte) -65, (byte) 17, (byte) 79, (byte) 31, (byte) 25, (byte) -39, (byte) 95, (byte) 93, (byte) 1, (byte) -110, (byte) -103, (byte) -118, (byte) -38, (byte) -57, (byte) -58, (byte) -51, (byte) -79};
    private Activity f0a;
    private int f1c;
    private byte[] f2k = new byte[8];

    public void cc() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:1:0x0001
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.modifyBlocksTree(BlockProcessor.java:248)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler$1.run(JadxDecompiler.java:199)
*/
        /*
        r5 = this;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.ctf.food.F.cc():void");
    }

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
}
