public class FoodSolve {
    private static byte[] flag = new byte[]{(byte) -19, (byte) 116, (byte) 58, (byte) 108, (byte) -1, (byte) 33, (byte) 9, (byte) 61, (byte) -61, (byte) -37, (byte) 108, (byte) -123, (byte) 3, (byte) 35, (byte) 97, (byte) -10, (byte) -15, (byte) 15, (byte) -85, (byte) -66, (byte) -31, (byte) -65, (byte) 17, (byte) 79, (byte) 31, (byte) 25, (byte) -39, (byte) 95, (byte) 93, (byte) 1, (byte) -110, (byte) -103, (byte) -118, (byte) -38, (byte) -57, (byte) -58, (byte) -51, (byte) -79};
    
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
    
    public static void main(String[] args) {
        byte[] bArr = new byte[]{(byte) 26, (byte) 27, (byte) 30, (byte) 4, (byte) 21, (byte) 2, (byte) 18, (byte) 7};
        byte[] inter = new byte[]{(byte) 0x13, (byte) 0x11, (byte) 0x13, (byte) 0x3, (byte) 0x4, (byte) 0x3, (byte) 0x1, (byte) 0x5};
        byte[] f2k = new byte[8];
        
        for (int i = 0; i < 8; i++) {
            f2k[i] = (byte) (bArr[i] ^ inter[i]);
        }
        
        System.out.println(new String(m0(flag, f2k)));
    }
}