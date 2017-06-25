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


Our flag is `CTF{bacon_lettuce_tomato_lobster_soul}`
