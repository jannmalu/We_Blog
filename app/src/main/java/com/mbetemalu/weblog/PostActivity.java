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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PostActivity extends AppCompatActivity {

    //Declare the views
    private ImageButton image_button;
    private EditText text_title, text_subtitle;
    private Button post_button;

    //Declare an instance of the database reference
    private StorageReference myStorageReference;

    //Declare an instance of the database reference where we will be saving the post details
    private DatabaseReference myDatabaseReference;

    //Declare an instance of firebase authentication
    private FirebaseAuth myAuthentication;

    //Declare an instance of the database reference where we have user details
    private DatabaseReference myDatabaseUsers;

    //Declare a instance of currently logged in user
    private FirebaseUser myCurrentUser;

    //Declare and initialize a private final static int that will serve as our request code
    private static final int GALLERY_REQUEST_CODE = 2;

    //Declare an instance of uri for getting the image from our phone, initialize it to null
    private Uri uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        //Inflate the toolbar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        //Initialize view objects
        post_button = findViewById(R.id.post_done);
        text_subtitle = findViewById(R.id.post_sub_text);
        text_title = findViewById(R.id.post_text);

        //Initialize the storage reference
        myStorageReference = FirebaseStorage.getInstance().getReference();

        //Initialize the database reference /node where you will be storing posts
        myDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Posts");

        //Initialize an instance of Firebase Authentication
        myAuthentication = FirebaseAuth.getInstance();

        //Initialize the instance of the firebase user
        myCurrentUser = myAuthentication.getCurrentUser();

        //Get currently logged in user
        myDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users").child(myCurrentUser.getUid());

        //Initialize the image button
        image_button = findViewById(R.id.add_post);

        //Picking the image from the gallery
        image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_REQUEST_CODE);
            }
        });

        //Posting to Firebase
        post_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(PostActivity.this, "POSTING...", Toast.LENGTH_LONG).show();

                //Get title and sub text from the edit texts
                final String PostTitle = text_title.getText().toString().trim();
                final String PostSub = text_subtitle.getText().toString().trim();

                //Get the date and time of the post
                java.util.Calendar calendar = Calendar.getInstance();
                SimpleDateFormat currentDate = new SimpleDateFormat("dd-MM-yyyy");
                final String saveCurrentDate = currentDate.format(calendar.getTime());

                java.util.Calendar calendar1 = Calendar.getInstance();
                SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
                final String saveCurrentTime  = currentTime.format(calendar1.getTime());

                //Do a check for empty fields
                if(!TextUtils.isEmpty(PostSub) && !TextUtils.isEmpty(PostTitle)){
                    //Create a storage reference node, inside post_image storage reference where you will save the post image
                    StorageReference filePath = myStorageReference.child("post_images").child(uri.getLastPathSegment());

                    /*Call the putFile() method passing the post image the user set on the storage reference where you are uploading the image
                     * Further call addOnSuccessListener on the reference to listen if the upload task was successful, and get a snapshot of the uploaded image*/
                    filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //If the upload of the post image was successful get the download url
                            if(taskSnapshot.getMetadata() != null){
                                if (taskSnapshot.getMetadata().getReference() != null){

                                    //Get the download url from your storage use the methods getStorage() and getDownloadUrl()
                                    Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();

                                    //Call the method addOnSuccessListener to determine if we got the download uri
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            //Convert the uri to a string on success
                                            final String imageUrl = uri.toString();

                                            Toast.makeText(getApplicationContext(), "Successfully Uploaded", Toast.LENGTH_SHORT).show();

                                            //call the method push() to publish the values on the database reference
                                            final DatabaseReference newPost = myDatabaseReference.push();

                                            //Adding post contents to database reference, call addValueEventListener so as to set the values
                                            myDatabaseUsers.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    newPost.child("title").setValue(PostTitle);
                                                    newPost.child("subText").setValue(PostSub);
                                                    newPost.child("postImage").setValue(imageUrl);
                                                    newPost.child("uid").setValue(myCurrentUser.getUid());
                                                    newPost.child("time").setValue(saveCurrentTime);
                                                    newPost.child("date").setValue(saveCurrentDate);

                                                    //Get the profile photo and display name of the person posting
                                                    newPost.child("profilePhoto").setValue(snapshot.child("profilePhoto").getValue());
                                                    newPost.child("displayName").setValue(snapshot.child("displayName").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                //Launch the main activity after posting
                                                                Intent intent = new Intent(PostActivity.this, MainActivity.class);
                                                                startActivity(intent);
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
    //Image from the gallery result
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK){

            //Get the image selected by the user
            uri = data.getData();

            //Set the image
            image_button.setImageURI(uri);
        }
    }
}
