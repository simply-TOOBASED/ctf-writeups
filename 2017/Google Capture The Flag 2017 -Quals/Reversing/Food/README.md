**DISCLAIMER**: Solved after competition ended.

>Android is all about the desserts, but can you come up with the secret recipe to cook up a flag?<br><br>[food.apk](https://capturetheflag.withgoogle.com/attachment/9fbe2bb7e74937796b6d7eb734cdde808f3cecb7e8c4c6dcd066fbfe477e45b3)

If the link above doesn't work to download the apk, I uploaded the apk to this folder as [food.apk](food.apk). 

The first step with our apk (Android mobile app) is to decompile it. I normally use this site: http://www.javadecompilers.com/apk. You can view the files from the decompiled apk in the [food_source_from_JADX](food_source_from_JADX) folder.

For those of you who aren't familiar with Android apps, when you start an application, the `MainActivity` is normally the one that's launched first, but we can verify which Activity is launched by looking at the `AndroidManifest.xml`, which can be found [here](./food_source_from_JADX/AndroidManifest.xml).

Pay close attention to this:
```
<activity android:name="com.google.ctf.food.FoodActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```
The `android.intent.action.MAIN` and `android.intent.category.LAUNCHER` tell us that `com.google.ctf.food.FoodActivity` is the main activity of the application and the one that's launched first when you first open the app.

So now let's take a look at the [com.google.ctf.food.FoodActivity.java](./food_source_from_JADX/com/google/ctf/food/FoodActivity.java) file
```
package com.google.ctf.food;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class FoodActivity extends AppCompatActivity {
    public static Activity activity;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) C0174R.layout.activity_food);
        activity = this;
        System.loadLibrary("cook");
    }
}
```
So for this activity, `C0174R.layout.activity_food` is the layout that's used, let's look at the layout resource file and see if there's anything interesting in it. It will probably be located in [/res/layout/activity_food.xml](./food_source_from_JADX/res/layout/activity_food.xml). There's only a Relative Layout and nothing else interesting, so let's take a look at this line: `System.loadLibrary("cook");`.

According to the [Android Docs](https://developer.android.com/reference/java/lang/System.html#loadLibrary(java.lang.String)):

>Loads the native library specified by the libname argument. The libname argument must not contain any platform specific prefix, file extension or path. If a native library called libname is statically linked with the VM, then the JNI_OnLoad_libname function exported by the library is invoked. See the JNI Specification for more details. Otherwise, the libname argument is loaded from a system library location and mapped to a native library image in an implementation- dependent manner.<br><br>The call `System.loadLibrary(name)` is effectively equivalent to the call<br><br>`Runtime.getRuntime().loadLibrary(name)`

So essentially what the method is doing is loading a native library, and looking at this StackOverflow question: https://stackoverflow.com/questions/27421134/system-loadlibrary-couldnt-find-native-library-in-my-case, we see that if our argument is `"cook"`, then there's probably a `libcook.so` in the `/lib/` folder, which [there is!](./food_source_from_JADX/lib/x86/libcook.so)

**Note**: There's a `x86` and `armeabi` folder, I use the `libcook.so` in the `x86` folder, because when I decompile the file using IDA Pro, it's easier to view the pseudocode and assembly code.

So the `.so` tells us that it's a shared library file, and according to what the Android Docs said above, we're looking for a JNI_OnLoad_libname function cause that is what invoked, so the flag will probably be somewhere in that function.

So now we decompile the `libcook.so` file using IDA Pro. The psuedocode that IDA gives us is in the file [libcook.c](./libcook.c). On the side we can view the assembly code for the `JNI_OnLoad` function, so let's look at that.

![Imgur](http://i.imgur.com/cXZUAYo.png)

Initially, we see a lot of `mov` statements with large hex numbers, so it looks like those values are being put onto the stack. After these values are put on the stack, then the function `sub_680` is called. This assembly code corresponding to this statement in the pseudocode: `filename = (char *)sub_680(21, 32);`.

Now let's look at `sub_680`. The following is the psuedocode for the function:

```
_BYTE *__cdecl sub_680(int a1, char a2)
{
  _BYTE *v2; // ebp@1
  int v3; // ecx@1
  unsigned int v4; // edx@2

  v2 = malloc(2 * a1 + 1);
  v3 = 0;
  if ( a1 > 0 )
  {
    do
    {
      v4 = *((_DWORD *)&a2 + v3);
      v2[2 * v3] = ~((BYTE1(v4) | ~*(&a2 + 4 * v3)) & (v4 | ~BYTE1(v4)));
      v2[2 * v3++ + 1] = (v4 >> 16) ^ BYTE3(v4);
    }
    while ( v3 != a1 );
  }
  v2[2 * a1] = 0;
  return v2;
}
```

So analyzing the function we see a couple of things, first it frees up memory using `malloc`. Then it does a do-while loop in which we take the address of `a2` (one of the inputs), cast it to a `_DWORD` pointer, add `v3` to this pointer, and then dereference the pointer. This is typical psuedocode IDA will give you when looping through some kind of array, we don't see any array. Remember those `mov` statements from earlier? All of those values were added to the stack, and `v4` is basically just looping through all of those values. The loop runs `a1` times, and if we see how many values are added to the stack through the `mov` statements, it's 21, and we also see 21 as a parameter to the `sub_680` function, so that confirms that the loop is probably looping through the values we added to the stack

With the line `filename = (char *)sub_680(21, 32);`, `32` is what's passed to `a2`, so if we look at the stack and the assembly code, that's the `mov     [esp+0ECh+mode], 25410F20h` line (32 in hex is 0x20). Because the `char` type in c is only 1 byte, we can only pass one byte as the value of `a2`, that's the reason why 32 is used as opposed to 0x25410F20, so the last byte (0x20) is being passed as the parameter. But, as we see in the psuedocode, it casts the addess as a `_DWORD` pointer, so when we derefence it, `v4` will have the value of 0x25410F20, because that value can be stored in a `_DWORD`.

So `v4` is looping through the values we added to the stack, and then it's doing some computations and storing them where we freed memory using `malloc` and then the function returns `v2`. Let's try and rewrite the computations the function is doing in python along with those values we added to the stack just to see what numbers we get and attempt to convert those numbers to printable ASCII chars.

```
def byte3(val):
    return (val >> 24) & 0xff

def byte2(val):
    return (val >> 16) & 0xff

def byte1(val):
    return (val >> 8) & 0xff

def byte0(val):
    return val & 0xff

def dec(vals):
    string = '';
    for s in vals:
        news = '';
        news += chr(~((byte1(s) | ~byte0(s)) & (s | ~byte1(s))));
        news += chr(byte2(s) ^ byte3(s));
        string += news;
    return string;

vals = [0x25410F20, 0x10640564, 0x5B744120, 0x4650C68, 0x6675420, 0x0C6F5D72, 0x36E1A75, 0x47204A64, 0x0A650D62, 0x0A660265, 0x4F614520, 0x10640D6E, 0x4D634620, 0x6F096F, 0x4420046B, 0x86E5A75, 0x5691D74, 0x5320096C, 0x16724D62, 0x1377416F, 0x1D650B6E]
filename = dec(vals);
print filename
```

So to explain the code, the reason why `*(&a2 + 4 * v3))` turned into `byte0(s)` is because `a2` is a char, so it's only 1 byte, and when you add `4*v3`, which will take values of `0,4,8,..`, you're basically going up the stack 0 bytes, 4 bytes, 8 bytes, etc, which corresponds to the lowest byte of each value we added on the stack. Also this expression: `(v4 >> 16)` was changed to just `byte2(s)` because `v2` in the pseudocode is of type `_BYTE`, so that means all of the computations will only get an answer that's a byte, which will probably be the lower byte of the result. When we run the python script we get the following: `/data/data/com.google.ctf.food/files/d.dex`. So this look like a file that might be hidden somewhere in this apk.

We look further in the pseudocode and see this line: `fwrite("dex\n035", 0x15A8u, 1u, v5)`, and `v5 = fopen(filename, v4)`, so we open the file `/data/data/com.google.ctf.food/files/d.dex` and we're writing data to it. We can double click on the `"dex\n035"` in IDA and see that it's at address `0x00001640`, so we copy the data from `0x00001640` to `0x00001640 + 0x15A8` and save that file as [d.dex](d.dex).

Searching online, a `.dex` file is nothing but a compiled Android program. All dex files are then zipped into one apk, and that's how an Android app is made. So we can decompile the `d.dex` file we have using an online tool again. This site: http://www.javadecompilers.com/apk works pretty well, and you can view the files from the decompiled d.dex file in the [d.dex_source_from_JADX](d.dex_source_from_JADX) folder.

We have 4 java files with weird names, but the [C0000F.java](./d.dex_source_from_JADX/com/google/ctf/food/C0000F.java) is interesting.

```
public class C0000F extends BroadcastReceiver {
    private static byte[] flag = new byte[]{(byte) -19, (byte) 116, (byte) 58, (byte) 108, (byte) -1, (byte) 33, (byte) 9, (byte) 61, (byte) -61, (byte) -37, (byte) 108, (byte) -123, (byte) 3, (byte) 35, (byte) 97, (byte) -10, (byte) -15, (byte) 15, (byte) -85, (byte) -66, (byte) -31, (byte) -65, (byte) 17, (byte) 79, (byte) 31, (byte) 25, (byte) -39, (byte) 95, (byte) 93, (byte) 1, (byte) -110, (byte) -103, (byte) -118, (byte) -38, (byte) -57, (byte) -58, (byte) -51, (byte) -79};
    private Activity f0a;
    private int f1c;
    private byte[] f2k = new byte[8];

    public void cc() {
        /* JADX: method processing error */
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
```

We see a `byte[]` array called "flag", but none of the bytes there are printable ascii chars, so we probably have to apply some kind of transformation to it. Also it seems one of the methods: `cc()` wasn't able to decompile properly. If we look at the other 3 java files, we don't see any reference to the `byte[] flag` from this java file. So what do we do now?

Let's look a little further in the pseudocode. We see these 3 lines:

```
remove(filename),
remove(v37),
rmdir(path),
```

So it looks like it removes the `d.dex` file, and then close to the end of the function it calls `sub_710()`. Let's take a look at that function.

```
signed int sub_710()
{
  const char *v0; // esi@1
  const char *v1; // eax@1
  const char *v2; // eax@3
  signed int result; // eax@5
  __int32 v4; // esi@8
  _BYTE *v5; // eax@8
  void *v6; // edi@8
  int v7; // eax@8
  void *v8; // edx@9
  char *v9; // edi@17
  int v10; // esi@17
  char v11; // al@18
  int v12; // edx@18
  FILE *v13; // [sp+28h] [bp-1C4h]@1
  _BYTE *v14; // [sp+2Ch] [bp-1C0h]@8
  char nptr[4]; // [sp+33h] [bp-1B9h]@8
  int v16; // [sp+37h] [bp-1B5h]@8
  char v17; // [sp+3Bh] [bp-1B1h]@8
  char v18; // [sp+3Ch] [bp-1B0h]@17
  char haystack; // [sp+CCh] [bp-120h]@3
  int v20; // [sp+D0h] [bp-11Ch]@8
  int v21; // [sp+1CCh] [bp-20h]@1

  v21 = _stack_chk_guard;
  v0 = sub_680(1, 114);
  v1 = sub_680(8, 72);
  v13 = fopen(v1, v0);
  if ( v13 )
  {
    do
    {
      if ( !fgets(&haystack, 256, v13) )
        goto LABEL_5;
      v2 = sub_680(3, 101);
    }
    while ( !strstr(&haystack, v2) );
    v17 = 0;
    *(_DWORD *)nptr = *(_DWORD *)&haystack;
    v16 = v20;
    v4 = sysconf(40);
    v5 = (_BYTE *)strtoul(nptr, 0, 16);
    v14 = v5;
    v6 = v5;
    v7 = mprotect(v5, v4 * (1968 / v4 + 8), 7);
    if ( !v7 )
    {
      v8 = v6;
      if ( 8 * v4 > 0 )
      {
        while ( *(_BYTE *)v8 != 100
             || *((_BYTE *)v8 + 1) != 101
             || *((_BYTE *)v8 + 2) != 120
             || *((_BYTE *)v8 + 3) != 10
             || *((_BYTE *)v8 + 4) != 48 )
        {
          ++v7;
          v8 = (char *)v8 + 1;
          if ( v7 == 8 * v4 )
            goto LABEL_5;
        }
        qmemcpy(&v18, &unk_15A0, 0x90u);
        v9 = &v18;
        v10 = v7 - (_DWORD)&v18;
        do
        {
          v11 = *v9;
          v12 = (int)&(v9++)[v10];
          v14[v12 + 1824] = v11 ^ 0x5A;
        }
        while ( v9 != &haystack );
      }
LABEL_5:
      fclose(v13);
      result = 1;
      goto LABEL_6;
    }
  }
  result = 0;
LABEL_6:
  if ( v21 != _stack_chk_guard )
    sub_650();
  return result;
}
```


Our flag is `CTF{bacon_lettuce_tomato_lobster_soul}`
