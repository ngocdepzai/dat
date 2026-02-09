package hc.manager.datapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import hc.manager.datapp.R;

public class SignInActivity extends AppCompatActivity {
    Button btLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btLogin = (Button) findViewById(R.id.btLogin);
        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(SignInActivity.this, MenuActivity.class);
                    intent.putExtra("Lat", 0);
                    intent.putExtra("Lng", 0);
                    startActivity(intent);
                } finally {
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                Intent intentLogin = new Intent(SignInActivity.this, LoginActivity.class);
                startActivity(intentLogin);
            } finally {
                finish();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}