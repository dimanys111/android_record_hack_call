package com.dima;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

    EditText editText;
    public static EditText editText_put=null;
    Button but;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // и без заголовка
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);
        editText_put = (EditText) findViewById(R.id.editText2);
        but = (Button) findViewById(R.id.button);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int i=Integer.parseInt(s.toString());
                if (WalkingIconService.Ser!=null)
                {
                    WalkingIconService.Ser.tim=i*1000;
                }
            }
        });
        if (WalkingIconService.Ser==null)
            startService(new Intent(this, WalkingIconService.class));
        else
        {
            WalkingIconService.Ser.otprSoket();
            editText_put.setText(WalkingIconService.Ser.dir.getPath());
            int i=WalkingIconService.Ser.tim/1000;
            String s=String.valueOf(i);
            editText.setText(s);
        }
    }

    @Override
    protected void onDestroy() {
        editText_put=null;
        super.onDestroy();
    }

    public void onMyButtonClick(View view)
    {
        if (WalkingIconService.Ser!=null)
        {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }

    private static long back_pressed;

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            if (WalkingIconService.Ser!=null) {
                WalkingIconService.Ser.tim_bool = false;
            }
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }

    public void onMyButtonClick_Plus(View view)
    {
        String s=editText.getText().toString();
        int i=Integer.parseInt(s)+1;
        s=String.valueOf(i);
        editText.setText(s);
    }

    public void onMyButtonClick_Min(View view)
    {
        String s=editText.getText().toString();
        int i=Integer.parseInt(s)-1;
        if (i<0)
            i=0;
        s=String.valueOf(i);
        editText.setText(s);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
