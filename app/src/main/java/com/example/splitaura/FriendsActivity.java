package com.example.splitaura;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.UserAdapter;
import com.example.splitaura.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FriendsActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserAdapter adapter;
    private RecyclerView recyclerView;
    private EditText searchView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_activity);

        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load all users initially
        loadUsers("");

        // Listen for text changes for dynamic search
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO document why this method is empty
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadUsers(String searchText) {
        Query query;
        if (searchText.isEmpty()) {
            query = db.collection("users");
        } else {
            // Make sure the search text is lowercase to match the stored keywords
            query = db.collection("users")
                    .whereArrayContains("searchKeywords", searchText.toLowerCase());
        }

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new UserAdapter(options);
        recyclerView.setAdapter(adapter);

        adapter.setOnUserRequestClickListener(snapshot -> {
            snapshot.getId();
            String fullName = snapshot.getString("fname") + " " + snapshot.getString("lname");
            // TODO: Add logic to send a request (e.g., updating Firestore)
            Toast.makeText(FriendsActivity.this, "Request sent to " + fullName, Toast.LENGTH_SHORT).show();
        });

        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}
