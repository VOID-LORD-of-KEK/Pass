package com.example.pass;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.pass.db.AppDatabase;
import com.example.pass.db.User;

public class AddNewUser extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_user);

        final EditText usernameInput =  findViewById(R.id.username);
        final EditText passwordInput =  findViewById(R.id.password);
        Button saveButton =  findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewUser(usernameInput.getText().toString(), passwordInput.getText().toString());
            }
        });
    }

    private void saveNewUser(String UserName, String PassWord) {
        AppDatabase db  = AppDatabase.getdbInstance(this.getApplicationContext());

        User user = new User();
        user.username = UserName;
        user.password = PassWord;
        db.userDao().insertUser(user);

        finish();

    }
}