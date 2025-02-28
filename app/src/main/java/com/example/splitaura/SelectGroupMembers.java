package com.example.splitaura;  // adjust your package name as needed

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.ContactsAdapter;
import com.example.splitaura.adapters.SelectedContactsAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.models.Contact;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectGroupMembers extends BaseActivity {

    private RecyclerView rvContacts, rvSelectedContacts;
    private EditText etSearch;
    private MaterialButton btnCancel, btnSave;
    private String title, description, currency, category, imageUrl;

    // Master list for all contacts and the ones selected by the user
    private List<Contact> allContacts = new ArrayList<>();
    // This list will hold the current (filtered) contacts for display
    private List<Contact> contactList = new ArrayList<>();
    private List<Contact> selectedContacts = new ArrayList<>();

    private ContactsAdapter contactsAdapter;
    private SelectedContactsAdapter selectedContactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members); // your main XML design

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // Set the status bar color to transparent
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }


        intializeViews();

        // Setup the contacts RecyclerView (vertical list)
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        contactsAdapter = new ContactsAdapter(contactList, new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact contact) {
                // Toggle selection
                if (!selectedContacts.contains(contact)) {
                    selectedContacts.add(contact);
                } else {
                    selectedContacts.remove(contact);
                }
                // Toggle visibility based on selection count
                rvSelectedContacts.setVisibility(selectedContacts.isEmpty() ? View.GONE : View.VISIBLE);
                selectedContactsAdapter.notifyDataSetChanged();
            }
        });
        rvContacts.setAdapter(contactsAdapter);

        // Setup the selected contacts RecyclerView (horizontal list)
        rvSelectedContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedContactsAdapter = new SelectedContactsAdapter(selectedContacts);
        rvSelectedContacts.setAdapter(selectedContactsAdapter);

        // Load test data (simulate Firestore fetching)
        loadTestData();

        // Add TextWatcher to enable search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });

        // Handle Cancel and Save button actions
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // simply exit the activity
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadDataToFirestore(title, description, currency, category, imageUrl, selectedContacts);
                // For now, just show a Toast with the number of selected contacts.
                Toast.makeText(SelectGroupMembers.this, "Selected " + selectedContacts.size() + " contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void intializeViews() {
        // Initialize views
        rvContacts = findViewById(R.id.rvContacts);
        rvSelectedContacts = findViewById(R.id.rvSelectedContacts);
        etSearch = findViewById(R.id.etSearch);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        currency = getIntent().getStringExtra("currency");
        category = getIntent().getStringExtra("category");
        imageUrl = getIntent().getStringExtra("imageUrl");
    }

    // Method to filter contacts based on search query
    private void filterContacts(String query) {
        List<Contact> filteredList = new ArrayList<>();
        for (Contact contact : allContacts) {
            if (contact.name.toLowerCase().contains(query.toLowerCase()) || contact.email.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(contact);
            }
        }
        // Update the adapter with the filtered list
        contactsAdapter.updateList(filteredList);
    }

    // Simulate fetching data from Firestore with test data
    private void loadTestData() {
        // Populate the master list with test data
        allContacts.add(new Contact("Vasu Zadafiya", "john@example.com", R.drawable.image));
        allContacts.add(new Contact("Jenish Sutariya", "jane@example.com", R.drawable.obmensmall));
        allContacts.add(new Contact("Sensi Vaghani", "alice@example.com", R.drawable.obwomens));
        allContacts.add(new Contact("Boby girl", "bob@example.com", R.drawable.obwomensmall));
        // Duplicate entries for testing search and scroll
        allContacts.add(new Contact("Justine", "john@example .com", R.drawable.image));
        allContacts.add(new Contact("Sahil", "jane@example .com", R.drawable.obmensmall));
        allContacts.add(new Contact("Trushali", "alice@example .com", R.drawable.obwomens));
        allContacts.add(new Contact("meera", "bob@example .com", R.drawable.obwomensmall));

        // Initially, display all contacts
        contactList.clear();
        contactList.addAll(allContacts);
        contactsAdapter.notifyDataSetChanged();
    }

    // Method to store group data (including the image URL) in Firestore
    private void uploadDataToFirestore(String title, String description, String currency, String category, String imageUrl, List<Contact> selectedContacts) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("title", title);
        groupData.put("description", description);
        groupData.put("currency", currency);
        groupData.put("category", category);
        groupData.put("imageUrl", imageUrl);
        groupData.put("members", selectedContacts);


        db.collection("users").document(user.getUid()).collection("groups").add(groupData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Firestore error: " + e.getMessage());
        });
    }
}
