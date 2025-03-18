package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splitaura.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_USER_UID = "user_uid";

    public static ProfileBottomSheetDialog newInstance(String userUid) {
        ProfileBottomSheetDialog fragment = new ProfileBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID, userUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_bottom_sheet_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userUid = getArguments() != null ? getArguments().getString(ARG_USER_UID) : null;
        if (userUid != null) {
            Fragment profileFragment = ProfileFragment.newInstance(userUid);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.bottom_sheet_fragment_container, profileFragment)
                    .commit();
        }

        // Optional: Adjust the height of the bottom sheet
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(0); // Adjust peek height as needed
            behavior.setDraggable(false);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}