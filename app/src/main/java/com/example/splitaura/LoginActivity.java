package com.example.splitaura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.utils.Utill;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LoginActivity extends BaseActivity {

    // For sign-in reference variables
    EditText fname;
    EditText lname;
    EditText email;
    EditText password;
    EditText confirmpassword;
    Button createac;
    private FirebaseAuth mAuth;
    TextView signup;
    TextView login;
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    LottieAnimationView glogin;

    // For Login Credential
    EditText lemail;
    EditText lpassword;
    Button loginBtn;
    String defaultImageURL;
    TextView forgetPassword;

    // For custom LoginDialogue
    private LoadingDialog loadingDialog;
    private GoogleSignInClient mGoogleSignInClient;

    // Dynamic Layout Credential
    ConstraintLayout signupConstraint;
    ConstraintLayout loginConstraint;

    // NestedScrollViews for both screens
    private NestedScrollView loginScrollView;
    private NestedScrollView signupScrollView;

    // Keyboard visibility tracking
    private boolean isKeyboardVisible = false;
    private int originalPaddingBottom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        signupInitialize();
        loginInitialize();

        // Find the NestedScrollViews
        loginScrollView = findViewById(R.id.login_scroll_view);
        signupScrollView = findViewById(R.id.signup_scroll_view);

        // Store the original padding
        originalPaddingBottom = loginScrollView.getPaddingBottom();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        createac.setOnClickListener(view -> signupuser(fname, lname, email, password, confirmpassword));
        loginBtn.setOnClickListener(view -> loginuser(lemail, lpassword));

        // Dynamic Views changes
        signup.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.VISIBLE);
            loginConstraint.setVisibility(View.GONE);
        });
        login.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.GONE);
            loginConstraint.setVisibility(View.VISIBLE);
        });
        glogin.setOnClickListener(view -> signIn());
        forgetPassword.setOnClickListener(view -> forgetPass());

        // Set up keyboard visibility listener
        setupKeyboardVisibilityListener();
    }

    private void setupKeyboardVisibilityListener() {
        final View rootView = findViewById(R.id.main);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is a heuristic for keyboard visibility
                    if (!isKeyboardVisible) {
                        isKeyboardVisible = true;
                        adjustLayoutForKeyboard(true, keypadHeight);
                    }
                } else {
                    if (isKeyboardVisible) {
                        isKeyboardVisible = false;
                        adjustLayoutForKeyboard(false, 0);
                    }
                }
            }
        });

        // Ensure focused EditText is scrolled into view
        for (EditText editText : new EditText[]{lemail, lpassword, fname, lname, email, password, confirmpassword}) {
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && isKeyboardVisible) {
                    scrollToFocusedView(editText);
                }
            });
        }
    }

    private void adjustLayoutForKeyboard(boolean keyboardVisible, int keyboardHeight) {
        NestedScrollView activeScrollView = (loginConstraint.getVisibility() == View.VISIBLE) ? loginScrollView : signupScrollView;
        if (activeScrollView == null) return;

        if (keyboardVisible) {
            activeScrollView.setPadding(
                    activeScrollView.getPaddingLeft(),
                    activeScrollView.getPaddingTop(),
                    activeScrollView.getPaddingRight(),
                    originalPaddingBottom + keyboardHeight / 2
            );
            EditText focusedEditText = findFocusedEditText();
            if (focusedEditText != null) {
                scrollToFocusedView(focusedEditText);
            }
        } else {
            activeScrollView.setPadding(
                    activeScrollView.getPaddingLeft(),
                    activeScrollView.getPaddingTop(),
                    activeScrollView.getPaddingRight(),
                    originalPaddingBottom
            );
        }
    }

    private EditText findFocusedEditText() {
        for (EditText editText : new EditText[]{lemail, lpassword, fname, lname, email, password, confirmpassword}) {
            if (editText.hasFocus()) {
                return editText;
            }
        }
        return null;
    }

    private void scrollToFocusedView(final EditText editText) {
        NestedScrollView activeScrollView = (loginConstraint.getVisibility() == View.VISIBLE) ? loginScrollView : signupScrollView;
        if (activeScrollView == null) return;

        activeScrollView.post(() -> {
            int[] location = new int[2];
            editText.getLocationInWindow(location);
            int editTextTop = location[1];
            int editTextBottom = editTextTop + editText.getHeight();

            Rect visibleRect = new Rect();
            activeScrollView.getWindowVisibleDisplayFrame(visibleRect);
            int visibleBottom = visibleRect.bottom;

            int keyboardHeight = getKeyboardHeight();
            int desiredTop = visibleBottom - keyboardHeight - editText.getHeight() - dpToPx(10);

            int scrollOffset = editTextTop - desiredTop;
            if (scrollOffset > 0) {
                activeScrollView.smoothScrollTo(0, activeScrollView.getScrollY() + scrollOffset);
            } else {
                activeScrollView.smoothScrollTo(0, editTextTop - dpToPx(10));
            }
        });
    }

    private int getKeyboardHeight() {
        Rect r = new Rect();
        View rootView = findViewById(R.id.main);
        rootView.getWindowVisibleDisplayFrame(r);
        int screenHeight = rootView.getRootView().getHeight();
        return screenHeight - r.bottom;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void forgetPass() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.reset_password, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        EditText emailEditText = dialogView.findViewById(R.id.edit_email);
        Button sendMail = dialogView.findViewById(R.id.btn_forget);
        ImageButton back = dialogView.findViewById(R.id.back_navigation);

        sendMail.setOnClickListener(view -> {
            String email = emailEditText.getText().toString().trim().toLowerCase();
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Please Enter your Email!");
                emailEditText.requestFocus();
                return;
            }

            loadingDialog.show();
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        loadingDialog.hide();
                        if (task.isSuccessful()) {
                            Utill.showToast(LoginActivity.this, "Reset email sent! Check your inbox.");
                            dialog.dismiss();
                        } else {
                            String errorMessage = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Error sending reset email";
                            if (errorMessage.contains("ERROR_USER_NOT_FOUND")) {
                                Utill.showToast(LoginActivity.this, "User Not Found! Please check your email.");
                            } else {
                                Utill.showToast(LoginActivity.this, "Error: " + errorMessage);
                            }
                        }
                    });
        });

        back.setOnClickListener(v -> dialog.dismiss());
    }

    private void loginInitialize() {
        lemail = findViewById(R.id.email_edit_text);
        lpassword = findViewById(R.id.password_edit_text);
        loginBtn = findViewById(R.id.login_button);
        signupConstraint = findViewById(R.id.signin_constarint);
        loginConstraint = findViewById(R.id.login_constarint);
        loadingDialog = new LoadingDialog(this);
        login = findViewById(R.id.log_in);
        forgetPassword = findViewById(R.id.forgot_password);
    }

    private void signupInitialize() {
        fname = findViewById(R.id.fname_edit_text);
        lname = findViewById(R.id.lanme_edit_text);
        email = findViewById(R.id.email1_edit_text);
        password = findViewById(R.id.password1_edit_text);
        createac = findViewById(R.id.create_acc_button);
        signup = findViewById(R.id.sign_up);
        confirmpassword = findViewById(R.id.confirm_password1_edit_text);
        defaultImageURL = getRandomUrl();
        glogin = findViewById(R.id.google_login);
    }

    private String getRandomUrl() {
        return RandomUrlHolder.URLS.get(RandomUrlHolder.RANDOM.nextInt(RandomUrlHolder.URLS.size()));
    }

    private void signIn() {
        loadingDialog.show();
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Utill.showToast(LoginActivity.this, "Google sign-in failed");
                loadingDialog.hide();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    String googleEmail = user.getEmail();
                    String displayName = user.getDisplayName();

                    String firstName = (displayName != null && displayName.contains(" "))
                            ? displayName.split(" ")[0] : displayName;
                    String lastName = (displayName != null && displayName.contains(" "))
                            ? displayName.split(" ")[1] : "";

                    Uri photoUrl = user.getPhotoUrl();
                    String googleProfilePictureUrl = (photoUrl != null) ? photoUrl.toString() : null;

                    // Check Firestore for existing user data
                    checkExistingUserData(uid, firstName, lastName, googleEmail, googleProfilePictureUrl);
                } else {
                    loadingDialog.hide();
                    Utill.showToast(LoginActivity.this, "Authentication Failed: User is null");
                }
            } else {
                loadingDialog.hide();
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Utill.showToast(LoginActivity.this, "Authentication Failed: " + task.getException());
            }
        });
    }

    private void checkExistingUserData(String uid, String firstName, String lastName, String email, String googleProfilePictureUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get().addOnCompleteListener(task -> {
            String profilePictureToUse;
            boolean isGoogleLogin = true;

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                String existingProfilePicture = document.getString("profile_picture");

                // Check if the existing profile picture is a custom image (e.g., from Cloudinary)
                if (existingProfilePicture != null && existingProfilePicture.contains("res.cloudinary.com")) {
                    // Preserve the custom image
                    profilePictureToUse = existingProfilePicture;
                    Log.d(TAG, "Preserving existing custom profile picture: " + profilePictureToUse);
                } else {
                    // No custom image; use Google profile picture if available, otherwise use a random image
                    profilePictureToUse = (googleProfilePictureUrl != null) ? googleProfilePictureUrl : getRandomUrl();
                    Log.d(TAG, "Using Google or random profile picture: " + profilePictureToUse);
                }
            } else {
                // User does not exist in Firestore; this is a new user
                profilePictureToUse = (googleProfilePictureUrl != null) ? googleProfilePictureUrl : getRandomUrl();
                Log.d(TAG, "New user, using Google or random profile picture: " + profilePictureToUse);
            }

            // Save or update user data in Firestore
            saveUserToFirestore(uid, firstName, lastName, email, null, profilePictureToUse, isGoogleLogin);
            navigateToHomepage(this);
        }).addOnFailureListener(e -> {
            loadingDialog.hide();
            Log.e(TAG, "Failed to fetch existing user data: ", e);
            Utill.showToast(LoginActivity.this, "Failed to fetch user data: " + e.getMessage());
        });
    }

    private void loginuser(EditText lemail, EditText lpassword) {
        String email = lemail.getText().toString();
        String password = lpassword.getText().toString();

        if (email.isEmpty()) {
            lemail.setError("Please Enter Email Address!");
            lemail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            lpassword.setError("Please Enter Password first!");
            lpassword.requestFocus();
            return;
        }
        loadingDialog.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Utill.showToast(this, "Login Successful");
                navigateToHomepage(this);
            } else {
                Utill.showToast(this, "User Not Found: " + task.getException());
            }
            loadingDialog.hide();
        });
    }

    private void signupuser(EditText fname, EditText lname, EditText lemail, EditText pass, EditText confirmpassword) {
        String firstname = fname.getText().toString();
        String lastname = lname.getText().toString();
        String email = lemail.getText().toString();
        String password = pass.getText().toString();
        String conpass = confirmpassword.getText().toString();

        if (validateinput(firstname, lastname, email, password, conpass)) {
            loadingDialog.show();
            signUpUserwithFirebase(firstname, lastname, email, password);
        }
    }

    private boolean validateinput(String firstname, String lastname, String mail, String pass, String conpass) {
        if (firstname.isEmpty()) {
            fname.setError("First Name is required");
            fname.requestFocus();
            return false;
        }
        if (lastname.isEmpty()) {
            lname.setError("Last Name is required");
            lname.requestFocus();
            return false;
        }
        if (mail.isEmpty()) {
            email.setError("Please Enter Email Address!");
            email.requestFocus();
            return false;
        }
        if (pass.isEmpty()) {
            password.setError("Please Enter Password!");
            password.requestFocus();
            return false;
        }
        if (!conpass.equals(pass)) {
            confirmpassword.setError("Password does not match");
            confirmpassword.requestFocus();
            return false;
        }
        return true;
    }

    private void signUpUserwithFirebase(String fname, String lname, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(fname + " " + lname)
                        .build();

                user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Utill.showToast(this, "User Profile Updated");
                        saveUserToFirestore(user.getUid(), fname, lname, email, password, defaultImageURL, false);
                        navigateToHomepage(this);
                    } else {
                        Utill.showToast(this, "User Profile Update Failed");
                    }
                    loadingDialog.hide();
                });
            } else {
                Utill.showToast(this, "Signup Failed: " + task.getException());
                loadingDialog.hide();
            }
        });
    }

    private void saveUserToFirestore(String uid, String fname, String lname, String email, String password, String profilePictureUrl, boolean isGoogleLogin) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                Log.e(TAG, "Fetching FCM registration token failed", tokenTask.getException());
                return;
            }
            String deviceToken = tokenTask.getResult();
            Log.d(TAG, "FCM Device Token: " + deviceToken);

            Map<String, Object> user = new HashMap<>();
            user.put("uid", uid);
            user.put("fname", fname);
            user.put("lname", lname);
            user.put("email", email);
            user.put("password", password);
            user.put("profile_picture", profilePictureUrl);
            user.put("gLogin", isGoogleLogin);

            List<String> searchKeywords = new ArrayList<>();
            searchKeywords.addAll(generateSearchKeywords(fname));
            searchKeywords.addAll(generateSearchKeywords(lname));
            user.put("searchKeywords", searchKeywords);

            db.collection("users")
                    .document(uid)
                    .set(user, SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.collection("users")
                                    .document(uid)
                                    .update("deviceTokens", FieldValue.arrayUnion(deviceToken))
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Log.d(TAG, "User saved and device token updated in Firestore.");
                                        } else {
                                            Log.e(TAG, "Error updating device token: ", updateTask.getException());
                                        }
                                    });
                        } else {
                            Log.e(TAG, "Error saving user: ", task.getException());
                        }
                    });
        });
    }

    private void navigateToHomepage(Activity activity) {
        loadingDialog.hide();
        Intent intent = new Intent(activity, Homepage.class);
        startActivity(intent);
        finish();
    }

    private List<String> generateSearchKeywords(String name) {
        List<String> keywords = new ArrayList<>();
        String lowerCaseName = name.toLowerCase().trim();
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < lowerCaseName.length(); i++) {
            prefix.append(lowerCaseName.charAt(i));
            keywords.add(prefix.toString());
        }
        return keywords;
    }

    private static class RandomUrlHolder {
        private static final List<String> URLS = Collections.unmodifiableList(Arrays.asList(
                "https://avatar.iran.liara.run/public/8",
                "https://avatar.iran.liara.run/public/37",
                "https://avatar.iran.liara.run/public/13",
                "https://avatar.iran.liara.run/public/31",
                "https://avatar.iran.liara.run/public/20",
                "https://avatar.iran.liara.run/public/12",
                "https://avatar.iran.liara.run/public/16",
                "https://avatar.iran.liara.run/public/64"
        ));
        private static final Random RANDOM = new Random();
    }
}