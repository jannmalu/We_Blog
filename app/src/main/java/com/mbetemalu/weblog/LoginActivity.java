package com.mbetemalu.weblog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    //Instance for the views
    private EditText login_email,login_pass;
    private FirebaseAuth myAuthentication;
    private DatabaseReference myDatabaseReference;
    private TextView not_user;
    private Button login_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Inflate the toolbar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        //Initialize the views
        login_button = findViewById(R.id.login_button);
        login_email = findViewById(R.id.login_email);
        login_pass = findViewById(R.id.login_password);
        not_user = findViewById(R.id.login_user);

        //Initialize the Firebase Authentication instance
        myAuthentication = FirebaseAuth.getInstance();

        //Initialize the database reference where you have the child node users
        myDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        not_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent login = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(login);
            }
        });
        //Set on click listener on the login button
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LoginActivity.this, "PROCESSING...", Toast.LENGTH_LONG).show();

                //Get the email and password and password entered by the user
                String email = login_email.getText().toString().trim();
                String password = login_pass.getText().toString().trim();

                if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)){
                    /*Use Firebase Authentication instance you create and call the method signWithEmailAndPassword() method passing
                     * the email and password you got from the views .
                     * Further call the addOnCompleteListener() method to handle the Authentication result*/
                    myAuthentication.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                //Create a method that will check if the user exists in the database reference
                                checkUserExistence();
                            }else{
                                //If the user does not exist in the database reference throw a toast
                                Toast.makeText(LoginActivity.this, "Couldn't login, User NOT Found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else{
                    //If the fields for the email and password are not complete show a toast
                    Toast.makeText(LoginActivity.this,"Complete All Fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    //This method checks iif the user exists
    public void checkUserExistence(){
        //Using the user id to check if the user exists
        final String user_id = myAuthentication.getCurrentUser().getUid();

        /*Call the method addValueEventListener on the database reference of the user to
        determine if the current userID supplied exists in the database reference*/
        myDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Get a snapshot of the users database reference to determine if current user exists
                if(snapshot.hasChild(user_id)){

                    //if the users exists direct the user to the main activity
                    Intent mainPage = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(mainPage);
                }else{

                    //If the user id does not exist show a toast
                    Toast.makeText(LoginActivity.this, "User Not Registered!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
