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

Our flag is `CTF{bacon_lettuce_tomato_lobster_soul}`
