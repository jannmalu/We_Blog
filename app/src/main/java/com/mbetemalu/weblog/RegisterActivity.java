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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    //Declare instances of the views
    private Button registerBtn;
    private EditText user_name, email_address, pass_word;
    private TextView login;

    //Instance for Firebase Authentication
    private FirebaseAuth myAuthentication;

    //Instance for Firebase Database
    private FirebaseDatabase myDatabase;

    //Declare an instance of Firebase Reference
    private DatabaseReference userReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        login = findViewById(R.id.user_existing);
        user_name = findViewById(R.id.username);
        email_address = findViewById(R.id.email);
        pass_word = findViewById(R.id.password);
        registerBtn = findViewById(R.id.register_button);

        //Initialize an instance of Firebase Authentication by calling the getInstance() method
        myAuthentication = FirebaseAuth.getInstance();

        //Initialize an instance of Firebase database by calling the getInstance() method
        myDatabase = FirebaseDatabase.getInstance();

        //Initialize an instance of Firebase Database reference by calling the databas instance, getting a reference using the get reference() method on the databasw, and creating a new child node, in this case "Users" where we will store details of registered users
        userReference = myDatabase.getReference().child("Users");

        /*For already registered users, they are redirected to the login page without directly registering them again
        An onClickListener is set on the text view in order that the user can be redirected to the Login Activity*/
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        /*An on click listener is set on the register button and on clicking this button we want to get the username,
         * email and password the user enters in the edit text fields then a new activity called Profile Activity is displayed where the users are allowed to set
         *  a custom display name and profile image*/

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(RegisterActivity.this, "LOADING...",Toast.LENGTH_LONG).show();
                //Get the data entered
                final String username = user_name.getText().toString().trim();
                final String email = email_address.getText().toString().trim();
                final String password = pass_word.getText().toString().trim();

                //Validate to ensure that the username and email address have been entered
                if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)){
                    /*Create a new createAccount method that takes in an email address and password, validates them and then creates a new user with the create user with email and password
                     * using the instance of authentication created and calls and on complete listener*/
                    myAuthentication.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //This is where the registered user will be stored in the database with a unique id
                            //Create a string variable to get the user id of the currently registered user

                            if(task.isSuccessful()){
                                String user_id = myAuthentication.getCurrentUser().getUid();

                                //Create a child node database reference to attach the user id to the users node
                                DatabaseReference current_user_db = userReference.child(user_id);

                                //Set the username and image on the users unique path
                                current_user_db.child("Username").setValue(username);
                                current_user_db.child("Image").setValue("Default");
                                //Toast to show successful registration
                                Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();

                                //Launch the Profile Activity for user to set their preference profile picture
                                Intent profileIntent = new Intent(RegisterActivity.this, ProfileActivity.class);
                                profileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(profileIntent);
                            }
                            else{
                                String error = task.getException().getMessage();
                                Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
                else{
                    Toast.makeText(RegisterActivity.this, "Complete all the fields",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
