package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendBalanceAdapter;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.model.FriendBalance;
import com.example.splitaura.model.GroupItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HomeFragment extends Fragment {

    private TextView username;
    private ImageView profilePicture;


    private static FirebaseAuth mAuth;
    private static String userId;
    private FirebaseFirestore db;
    private FirebaseFirestoreSettings settings;



    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        RecyclerView groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        RecyclerView friendRecycleView = view.findViewById(R.id.friend_recyclerView);
        friendRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));

//        layout instance
        username = view.findViewById(R.id.username_text);
        profilePicture = view.findViewById(R.id.profile_picture);

//        firebase instance
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        // Sample Data
        List<GroupItem> groupList = new ArrayList<>();
        groupList.add(new GroupItem(
                "Office Trip", "May 23, 2024", 9235.34,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.image, R.drawable.obwomens, R.drawable.obmensmall),
                12));

        groupList.add(new GroupItem(
                "Weekend Party", "Feb 10, 2024", 4850.50,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.obmensmall, R.drawable.image, R.drawable.obwomensmall),
                8));

        GroupAdapter adapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(adapter);


        List<FriendBalance> friendBalanceList = new ArrayList<>();
        friendBalanceList.add(new FriendBalance("Vasu Z.", -45.69, R.drawable.image));
        friendBalanceList.add(new FriendBalance("Sahil H.", 120.50, R.drawable.obwomens));
        friendBalanceList.add(new FriendBalance("Tushar C.", -230.75, R.drawable.obmensmall));
        friendBalanceList.add(new FriendBalance("Bhargav Z.", 90.00, R.drawable.obwomensmall));

        FriendBalanceAdapter friendBalanceAdapter = new FriendBalanceAdapter(getContext(), friendBalanceList);
        friendRecycleView.setAdapter(friendBalanceAdapter);

        setUserName();
        fetchProfilePicture();

        return view;
    }


    private void fetchProfilePicture() {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Retrieve the profile picture URL from Firestore
                    String profilePicUrl = document.getString("profile_picture");
                    // If the URL exists and is not empty, load the image into the ImageView
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profilePicUrl)
                                .into(profilePicture);
                    }
                    // If profilePicUrl is null or empty, do nothing
                    // so the static image defined in XML remains visible.
                }
            } else {
                // Optionally, log or handle errors here
            }
        });

    }

    //    for setting greeting message to user
    private void setUserName() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fname = documentSnapshot.getString("fname");
                        // Set the greeting message in one shot
                        if (fname != null) {
                            username.setText("Hello " + fname.toLowerCase() + "\nManage your finance easily");
                        } else {
                            // Handle missing fname field
                            username.setText("Hello User\nManage your finance easily");
                        }
                    } else {
                        // Handle case where user document doesn't exist
                        Log.e("Firestore", "User document does not exist for UID: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    Log.e("Firestore", "Error getting user document", e);
                });

    }


}