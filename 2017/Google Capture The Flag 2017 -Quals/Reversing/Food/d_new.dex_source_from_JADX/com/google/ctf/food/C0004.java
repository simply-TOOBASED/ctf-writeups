package com.google.ctf.food;

public class C0004 {
    public static byte[] m0(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[256];
        byte[] bArr4 = new byte[256];
        int i = 0;
        int i2 = 0;
        while (i2 != 256) {
            bArr3[i2] = (byte) i2;
            bArr4[i2] = bArr2[i2 % bArr2.length];
            i2++;
        }
        int i3 = i2 ^ i2;
        i2 = 0;
        while (i3 != 256) {
            i2 = ((i2 + bArr3[i3]) + bArr4[i3]) & 255;
            bArr3[i2] = (byte) (bArr3[i2] ^ bArr3[i3]);
            bArr3[i3] = (byte) (bArr3[i3] ^ bArr3[i2]);
            bArr3[i2] = (byte) (bArr3[i2] ^ bArr3[i3]);
            i3++;
        }
        bArr4 = new byte[bArr.length];
        i3 ^= i3;
        i2 ^= i2;
        while (i != bArr.length) {
            i3 = (i3 + 1) & 255;
            i2 = (i2 + bArr3[i3]) & 255;
            bArr3[i2] = (byte) (bArr3[i2] ^ bArr3[i3]);
            bArr3[i3] = (byte) (bArr3[i3] ^ bArr3[i2]);
            bArr3[i2] = (byte) (bArr3[i2] ^ bArr3[i3]);
            bArr4[i] = (byte) (bArr[i] ^ bArr3[(bArr3[i3] + bArr3[i2]) & 255]);
            i++;
        }
        return bArr4;
    }
}
