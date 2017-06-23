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
