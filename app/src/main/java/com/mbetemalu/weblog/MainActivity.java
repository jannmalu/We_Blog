package com.mbetemalu.weblog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference likesRef;
    private TextView share_button;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    Boolean likeChecker = true;
    private FirebaseRecyclerAdapter adapter;
    String currentUserID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //Inflate the toolbar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        
        recyclerView = findViewById(R.id.posted_recycler);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        
        //Reverse the layout to display the most recent post at the top
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        
        /*Get the database reference where you will fetch posts 
        * Initialize the database reference where you will store the likes*/
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        //Get an instance of the firebase authentication
        mAuth = FirebaseAuth.getInstance();
        
        //Get currently logged in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            //Launch the registration activity if user is not logged in
            Intent loginIntent = new Intent(MainActivity.this, RegisterActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        //Check to see if the user is logged in
        if(currentUser != null){
            
            //If user is logged in populate the interface with card views
            updateUI(currentUser);
            
            //Listen to the events on the adapter
            adapter.startListening();
        }
    }

    private void updateUI(final FirebaseUser currentUser) {
        //Create and initialize an instance of query that retrieves all posts uploaded
        Query query = FirebaseDatabase.getInstance().getReference().child("Posts");

        //Create and initialize an instance of the recycler options passing in your model class
        FirebaseRecyclerOptions<WeBlog> options = new FirebaseRecyclerOptions.Builder<WeBlog>().setQuery(query, new SnapshotParser<WeBlog>() {
            @NonNull
            @Override

            //Create a snapshot of the post
            public WeBlog parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new WeBlog(snapshot.child("title").getValue().toString(),
                        snapshot.child("subText").getValue().toString(),
                        snapshot.child("postImage").getValue().toString(),
                        snapshot.child("displayName").getValue().toString(),
                        snapshot.child("profilePhoto").getValue().toString(),
                        snapshot.child("time").getValue().toString(),
                        snapshot.child("date").getValue().toString()
                );
            }
        }).build();

        /*Create a firebase adapter passing in the model and a view holder
        * Create a new view holder as a public inner class that extends RecyclerView.Holder, outside the create, start and update ui methods
        * Implement the methods onCreateViewHolder and onBindViewHolder
        * Complete all the steps in the View Holder before proceeding to the methods onCreateViewHolder and onBindViewHolder */

        adapter = new FirebaseRecyclerAdapter<WeBlog, WeBlogViewHolder>(options) {

            @NonNull
            @Override
            public WeBlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                //Inflate the layout where you have the card view items
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_items,parent,false);
                return new WeBlogViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull WeBlogViewHolder holder, int position, @NonNull WeBlog model) {

                //Very important for you to get the post key since we will use this to set the likes and delete a particular post
                final String post_key = getRef(position).getKey();

                //Populate the card view with data
                holder.setPost_title(model.getTitle());
                holder.setPost_description(model.getDescription());
                holder.setPost_image(getApplicationContext(),model.getPostImage());
                holder.setPost_username(model.getDisplayName());
                holder.setUser_image(getApplicationContext(),model.getProfilePhoto());
                holder.setPost_time(model.getTime());
                holder.setPost_date(model.getDate());

                //Set a like on a particular post
                holder.setLikeButtonStatus(post_key);

                //Add an onClickListener on the particular post to allow opening this post on a different screen
                holder.post_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        /*Launch the screen single post activity on clicking a particular card view
                        * Create this activity using an empty activity template*/
                        Intent singlePost = new Intent(MainActivity.this, SinglePostActivity.class);
                        singlePost.putExtra("PostID", post_key);
                        startActivity(singlePost);
                    }
                });

                holder.share_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("text/plain");
                                share.putExtra(Intent.EXTRA_TEXT, "Check out this blog post");

                                Intent choose = Intent.createChooser(share, "Share Via");
                                if (share.resolveActivity(getPackageManager()) != null){
                                    startActivity(choose);
                                }

                    }
                });
                holder.post_like.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        /*Initialize the like checker to true, we are using this boolean variable to determine if a
                        * post is liked or disliked.*/
                        likeChecker = true;

                        //Check the currently logged i user using his/her ID
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null){
                            currentUserID = user.getUid();
                        }
                        else{
                            Toast.makeText(MainActivity.this,"Please Login", Toast.LENGTH_SHORT).show();
                        }

                        //Listen to any changes in the likes database reference
                        likesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (likeChecker.equals(true)){
                                    /*If the current post had a like associated to the current logged and the user clicks on it again,
                                    * remove the like , basically this means that the user is disliking the post*/
                                    if (snapshot.child(post_key).hasChild(currentUserID)){
                                        likesRef.child(post_key).child(currentUserID).removeValue();
                                        likeChecker = false;
                                    }else {

                                        //Here the user is liking , set the value on the  like
                                        likesRef.child(post_key).child(currentUserID).setValue(true);
                                        likeChecker = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            adapter.stopListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.add_post) {
            Intent postIntent = new Intent(this, PostActivity.class);
            startActivity(postIntent);
        }else if (id == R.id.log_out){
            mAuth.signOut();
            Intent logoutIntent = new Intent(MainActivity.this, RegisterActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(logoutIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    public class WeBlogViewHolder extends RecyclerView.ViewHolder {
        //Declare the view objects in the card view
        public TextView post_title;
        public TextView post_description;
        public ImageView post_image;
        public TextView post_username;
        public ImageView user_image;
        public TextView post_time;
        public TextView post_date;
        public LinearLayout post_layout;
        public ImageButton post_like,post_comment;
        public TextView display_likes;
        public TextView share_button;

        //Declare an it to hold the number of likes
        int countLikes;

        //Declare a string variable to hold the user ID of the currently logged in user
        String currentUserID;

        //Declare an instance for the firebase authentication
        FirebaseAuth mAuth;

        //Declare a database reference where the likes are saved
        DatabaseReference likesRef;

        public WeBlogViewHolder(@NonNull View itemView) {
            super(itemView);

            //Initialize the card view item objects
            post_title = itemView.findViewById(R.id.blogger_post_title);
            post_description = itemView.findViewById(R.id.blogger_post_description);
            post_image = itemView.findViewById(R.id.blogger_picture_post);
            post_username = itemView.findViewById(R.id.blogger_name);
            user_image = itemView.findViewById(R.id.blogger_profile_image);
            post_time = itemView.findViewById(R.id.time_posted);
            post_date = itemView.findViewById(R.id.date_posted);
            post_layout = itemView.findViewById(R.id.post_linear_layout);
            post_like = itemView.findViewById(R.id.like_button);
            post_comment = itemView.findViewById(R.id.comment);
            display_likes = itemView.findViewById(R.id.likes_display);
            share_button = itemView.findViewById(R.id.share);

            //Initialize a database reference where the likes will be stored
            likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        }
        //The Setters that will be used in the onBindViewHolder method
        public void setPost_title(String title){
            post_title.setText(title);
        }
        public void setPost_description(String description){
            post_description.setText(description);
        }
        public void setPost_image(Context context, String postImage){
            Picasso.with(context).load(postImage).into(post_image);
        }
        public void setPost_username(String userName){

            post_username.setText(userName);
        }
        public void setUser_image(Context context, String profilePhoto){
            Picasso.with(context).load(profilePhoto).into(user_image);
        }
        public void setPost_time(String time){
            post_time.setText(time);
        }
        public void setPost_date(String date){

            post_date.setText(date);
        }

        public void setLikeButtonStatus(final String post_key){

            //We want to know who has liked a particular post using their userID
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null){
                currentUserID = user.getUid();
            }
            else{
                Toast.makeText(MainActivity.this, "Please Login", Toast.LENGTH_SHORT).show();
            }

            //Listen to changes in the database reference of likes
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    /*Define post_key in the onBindViewHolder method
                     * Check if a particular post has been liked*/
                    if (snapshot.child(post_key).hasChild(currentUserID)){

                        //If liked get the number of likes
                        countLikes= (int) snapshot.child(post_key).getChildrenCount();

                        //Check the image from initial dislike to like
                        post_like.setImageResource(R.drawable.ic_dislike);

                        //Display the current number of likes
                        display_likes.setText(Integer.toString(countLikes));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

    }

}
