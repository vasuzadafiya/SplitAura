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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupViewModel extends AndroidViewModel {
    private final MutableLiveData<List<GroupItem>> groupListLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final FirebaseFirestore db;
    private final String userId;
    private ListenerRegistration groupsListener;
    private boolean isDataFetched = false;
    private final MutableLiveData<Boolean> groupDeleted = new MutableLiveData<>();

    public GroupViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (!isDataFetched) {
                fetchGroupsRealtime();
            }
        } else {
            groupListLiveData.setValue(new ArrayList<>());
            userId = null;
        }
    }

    public LiveData<List<GroupItem>> getGroupList() {
        return groupListLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    private void fetchGroupsRealtime() {
        if (isDataFetched) {
            return;
        }

        isLoadingLiveData.setValue(true);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        groupsListener = db.collection("groups")
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    isLoadingLiveData.setValue(false);

                    if (e != null) {
                        Log.e("GroupViewModel", "Error fetching groups: ", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<GroupItem> groups = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String groupId = document.getId();
                            String title = document.getString("title");
                            Date createdAtDate = document.getDate("createdAt");
                            String date = (createdAtDate != null) ? sdf.format(createdAtDate) : "";
                            Double amountObj = document.getDouble("amount");
                            double amount = (amountObj != null) ? amountObj : 0.0;
                            String imageUrl = document.getString("imageUrl"); // Fetch group image URL

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

                            Map<String, Double> participantsMap = (Map<String, Double>) document.get("participants");
                            if (participantsMap == null) {
                                participantsMap = new HashMap<>();
                            }

                            groups.add(new GroupItem(groupId, title, date, amount, imageUrl, memberImages, moreMembersCount, participantsMap));
                        }
                        groupListLiveData.setValue(groups);
                        isDataFetched = true;
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupsListener != null) {
            groupsListener.remove();
        }
    }

    public void setGroupDeleted(boolean deleted) {
        groupDeleted.setValue(deleted);
    }

    public MutableLiveData<Boolean> getGroupDeleted() {
        return groupDeleted;
    }
}