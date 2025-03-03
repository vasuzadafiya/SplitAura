package com.example.splitaura.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splitaura.models.GroupItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupViewModel extends AndroidViewModel {
    private final MutableLiveData<List<GroupItem>> groupListLiveData;
    private final FirebaseFirestore db;
    private final String userId;
    private ListenerRegistration groupsListener; // To manage the listener

    public GroupViewModel(@NonNull Application application) {
        super(application);
        groupListLiveData = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            fetchGroupsRealtime();
        } else {
            groupListLiveData.setValue(new ArrayList<>());
            userId = null;
        }
    }

    public LiveData<List<GroupItem>> getGroupList() {
        return groupListLiveData;
    }

    private void fetchGroupsRealtime() {
        // Create the date formatter only once
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        groupsListener = db.collection("users")
                .document(userId)
                .collection("groups")
                .addSnapshotListener((QuerySnapshot queryDocumentSnapshots,
                                      com.google.firebase.firestore.FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("GroupViewModel", "Error fetching groups: ", e);
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        List<GroupItem> groups = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String title = document.getString("title");

                            // Format createdAt field from Timestamp to "dd/MM/yyyy"
                            Date createdAtDate = document.getDate("createdAt");
                            String date = (createdAtDate != null) ? sdf.format(createdAtDate) : "";

                            Double amountObj = document.getDouble("amount");
                            double amount = (amountObj != null) ? amountObj : 0.0;
                            // In your current data, imageUrl is null; update accordingly if needed.
                            String imageUrl = null;

                            // Process "members" field stored as an ArrayList
                            List<Object> membersList = (List<Object>) document.get("members");
                            List<String> memberImages = new ArrayList<>();
                            int totalMembers = (membersList != null) ? membersList.size() : 0;

                            if (membersList != null) {
                                int count = 0;
                                for (Object memberObj : membersList) {
                                    if (memberObj instanceof Map) {
                                        Map<String, Object> memberData = (Map<String, Object>) memberObj;
                                        String profilePicUrl = (String) memberData.get("profile_picture");
                                        if (count < 3 && profilePicUrl != null) {
                                            memberImages.add(profilePicUrl);
                                        }
                                        count++;
                                    }
                                }
                            }
                            int moreMembersCount = (totalMembers > 3) ? totalMembers - 3 : 0;

                            groups.add(new GroupItem(title, date, amount, imageUrl, memberImages, moreMembersCount));
                        }
                        groupListLiveData.setValue(groups);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove the listener when ViewModel is cleared to prevent memory leaks.
        if (groupsListener != null) {
            groupsListener.remove();
        }
    }
}
