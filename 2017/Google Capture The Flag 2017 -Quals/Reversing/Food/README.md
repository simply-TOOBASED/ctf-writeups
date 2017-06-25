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
Our flag is `CTF{bacon_lettuce_tomato_lobster_soul}`
