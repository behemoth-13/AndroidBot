/*
 * Copyright 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.testing.uiautomator.BasicSample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * An {@link Activity} that gets a text string from the user and displays it back when the user
 * clicks on one of the two buttons. The first one shows it in the same activity and the second
 * one opens another activity and displays the message.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    // The TextView used to display the message inside the Activity.
    private TextView mTextView;

    // The EditText where the user types the message.
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the listeners for the buttons.
        findViewById(R.id.changeTextBt).setOnClickListener(this);
        findViewById(R.id.activityChangeTextBtn).setOnClickListener(this);

        mTextView = (TextView) findViewById(R.id.textToBeChanged);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getApplicationContext());
        int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        mTextView.setText("" + longPressTimeout);
        mEditText = (EditText) findViewById(R.id.editTextUserInput);
    }

    @Override
    public void onClick(View view) {
        // Get the text from the EditText view.
        Process rt = null;
        try {
            rt = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(rt.getOutputStream());
            File sdcard = Environment.getExternalStorageDirectory();
            ;

            os.writeBytes("uiautomator runtest " + sdcard.getAbsolutePath() + "Testing.jar" + " -c com.example.android.testing.uiautomator.BasicSample.ChangeTextBehaviorTest" + "\n");
            os.flush();
           // os.writeBytes("exit\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        final String text = mEditText.getText().toString();
//
//        switch (view.getId()) {
//            case R.id.changeTextBt:
//                // First button's interaction: set a text in a text view.
//                mTextView.setText(text);
//                break;
//            case R.id.activityChangeTextBtn:
//                // Second button's interaction: start an activity and send a message to it.
//                Intent intent = ShowTextActivity.newStartIntent(this, text);
//                startActivity(intent);
//                break;
//        }
    }
}
