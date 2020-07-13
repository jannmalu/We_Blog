package com.mbetemalu.weblog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {

    //Declare instances of the views
    private EditText profileUsername;
    private ImageButton imageButton;
    private Button doneButton;

    //Instance for Firebase Authentication
    private FirebaseAuth myAuthentication;

    //Instance for the database reference where the profile photo and display name will be saved
    private DatabaseReference myDatabaseUser;

    //Instance of the storage reference where we shall upload the photo
    private StorageReference myStorageReference;

    //Instance of Uri for getting the image from the phone and initialize to null
    private Uri profileImageUri = null;

    /*Since we want to get the result we will start the implicit intent using the method startActivityForResult()
     * startActivityForResult() method will require two arguments the intent and the request code*/

    //Declare and initialize a private final static int that will serve as our request code
    private final static int GALLERY_REQ = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Inflate the toolbar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        //Initialize the views
        profileUsername = findViewById(R.id.profile_text);
        imageButton = findViewById(R.id.add_picture);
        doneButton = findViewById(R.id.profile_done);

        //Initialize the instance of Firebase authentication
        myAuthentication = FirebaseAuth.getInstance();

        //We want to set the profile for specific, hence get the user id of the current user ans assign it to a string variable
        final String userID = myAuthentication.getCurrentUser().getUid();

        //Initialize the database reference where you have your registered users and get the specific user reference using the userID
        myDatabaseUser = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);

        //Initialize the firebase storage reference where you will store the profile photo images
        myStorageReference = FirebaseStorage.getInstance().getReference().child("profile_images");

        //An onClickListener on the image button so as to allow users to pick their profile images from the gallery
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Intent to get images only
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");

                //since we need results, use the method startActivityForResult() and pass the intent and request code you initialized
                startActivityForResult(galleryIntent,GALLERY_REQ);
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get the custom display name entered by the user
                final String name = profileUsername.getText().toString().trim();

                //Validate to ensure that the name and profile images are not null
                if(!TextUtils.isEmpty(name) && profileImageUri != null){

                    //Create Storage reference node, inside profile_image storage reference where you will save the profile image
                    StorageReference profileImagePath = myStorageReference.child("profile_images").child(profileImageUri.getLastPathSegment());

                    /*Call the putFile() method passing the profile image the user set on the storage reference where you are uploading the image
                     * Further call add on success listener on the reference to listen  if the upload task was successful and get a snapshot of the task*/
                    profileImagePath.putFile(profileImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            //If the upload of the profile image was successful get the download url
                            if(taskSnapshot.getMetadata()!=null){
                                if (taskSnapshot.getMetadata().getReference()!=null){

                                    //get the download url from the storage, use the methods getStorage() and getDownloadUrl()
                                    Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();

                                    //Call the on success listener method to determine if we got the download url
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            final String profileImage = uri.toString();

                                            //call the method push() to add values on the database reference of a specific user
                                            myDatabaseUser.push();

                                            //Call the method add value event listener to publish the additions in the database reference of a specific user
                                            myDatabaseUser.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                    //add the profile photo and display name for the current user
                                                    myDatabaseUser.child("displayName").setValue(name);
                                                    myDatabaseUser.child("profilePhoto").setValue(profileImage).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){

                                                                //show a toast to indicate the profile was updated
                                                                Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();

                                                                //Launch the Login Activity
                                                                Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
                                                                startActivity(loginIntent);
                                                            }
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    //Override this method to get the profile image set it in image button view
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQ && resultCode == RESULT_OK){

            //Get the image selected by the user
            profileImageUri = data.getData();

            //Set in the image button view
            imageButton.setImageURI(profileImageUri);
        }
    }
}
