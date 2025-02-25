package com.example.splitaura;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    //    for sign-in reference variables
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

    //    for Login Credential
    EditText lemail;
    EditText lpassword;
    Button loginBtn;
    String defaultImageURL = "https://www.w3schools.com/howto/img_avatar.png";

    //    for custom LoginDialogue
    private LoadingDialog loadingDialog;
    private GoogleSignInClient mGoogleSignInClient;

    //    Dynamic Layout Credential
    ConstraintLayout signupConstraint;
    ConstraintLayout loginConstraint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        //        initialize signup variable
        mAuth = FirebaseAuth.getInstance();
        fname = findViewById(R.id.fname_edit_text);
        lname = findViewById(R.id.lanme_edit_text);
        email = findViewById(R.id.email1_edit_text);
        password = findViewById(R.id.password1_edit_text);
        createac = findViewById(R.id.create_acc_button);
        signup = findViewById(R.id.sign_up); //textView
        confirmpassword = findViewById(R.id.confirm_password1_edit_text);


        //        initialize Login variables
        lemail = findViewById(R.id.email_edit_text);
        lpassword = findViewById(R.id.password_edit_text);
        loginBtn = findViewById(R.id.login_button);
        signupConstraint = findViewById(R.id.signin_constarint);
        loginConstraint = findViewById(R.id.login_constarint);
        loadingDialog = new LoadingDialog(this);
        login = findViewById(R.id.log_in);

//    google login variable
        glogin = findViewById(R.id.google_login);
//          Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        createac.setOnClickListener(view -> signupuser(fname, lname, email, password, confirmpassword));

        loginBtn.setOnClickListener(view -> loginuser(lemail, lpassword));


//        Dynamic Views changes
        signup.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.VISIBLE);
            loginConstraint.setVisibility(View.GONE);
        });
        login.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.GONE);
            loginConstraint.setVisibility(View.VISIBLE);
        });

        glogin.setOnClickListener(view -> signIn());

    }

//    private void signIn() {
//        mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
//            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//            startActivityForResult(signInIntent, RC_SIGN_IN);
//        });
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // If the requestCode matches, handle the result
//        if (requestCode == RC_SIGN_IN) {
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            try {
//                // Handle Google Sign-In Success
//                GoogleSignInAccount account = task.getResult(ApiException.class);
//                firebaseAuthWithGoogle(account.getIdToken());
//            } catch (ApiException e) {
//                // If Google Sign-In fails, log the exception
//                Log.w(TAG, "Google sign in failed", e);
//                Utill.showToast(LoginActivity.this, "Google sign-in failed");
//            }
//        }
//    }
//
//    private void firebaseAuthWithGoogle(String idToken) {
//        loadingDialog.show();
//        // Step 1: Get Firebase credentials from Google
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//
//        // Step 2: Sign in with Firebase using credentials
//        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
//            if (task.isSuccessful()) {
//                // Step 3: If successful, get current user and details
//                FirebaseUser user = mAuth.getCurrentUser();
//                assert user != null;
//                String uid = user.getUid();
//                String googleId = user.getEmail();
//                String displayName = user.getDisplayName();
//
//                // Split display name into first and last names if necessary
//                String firstName = (displayName != null && displayName.contains(" "))
//                        ? displayName.split(" ")[0] : displayName;
//                String lastName = (displayName != null && displayName.contains(" "))
//                        ? displayName.split(" ")[1] : "";
//
//                Log.d(TAG, "First Name: " + firstName);
//                Log.d(TAG, "Last Name: " + lastName);
//                Log.d(TAG, "Google ID: " + googleId);
//
//
//                saveUserToFirestore(uid, firstName, lastName, googleId, null);
//                createDialoge(LoginActivity.this, true); // Show new user dialog
//                Log.d(TAG, "New user registration process completed.");
//
//            } else {
//                // Handle sign-in failure
//                Log.w(TAG, "signInWithCredential:failure", task.getException());
//                Utill.showToast(LoginActivity.this, "Authentication Failed: " + task.getException());
//            }
//            loadingDialog.hide();
//        });
//    }

    // Initiate Google Sign-In
    private void signIn() {
        // Optional: Revoke previous access if needed
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

//    private void firebaseAuthWithGoogle(String idToken) {
//        loadingDialog.show();
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//        mAuth.signInWithCredential(credential)
//                .addOnCompleteListener(this, task -> {
//                    try {
//                        if (task.isSuccessful()) {
//                            FirebaseUser user = mAuth.getCurrentUser();
//                            if (user != null) {
//                                String uid = user.getUid();
//                                String email = user.getEmail();
//                                String displayName = user.getDisplayName();
//
//                                // Robust splitting: first word is firstName, rest is lastName
//                                String firstName = displayName;
//                                String lastName = "";
//                                if (displayName != null && displayName.contains(" ")) {
//                                    String[] nameParts = displayName.split(" ");
//                                    firstName = nameParts[0];
//                                    if (nameParts.length > 1) {
//                                        lastName = TextUtils.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length));
//                                    }
//                                }
//
//                                Log.d(TAG, "First Name: " + firstName);
//                                Log.d(TAG, "Last Name: " + lastName);
//                                Log.d(TAG, "Google Email: " + email);
//
//                                saveUserToFirestore(uid, firstName, lastName, email, null);
//                                createDialoge(LoginActivity.this, true); // Show new user dialog
//                                Log.d(TAG, "New user registration process completed.");
//                            }
//                        } else {
//                            Log.w(TAG, "signInWithCredential:failure", task.getException());
//                            Utill.showToast(LoginActivity.this, "Authentication Failed: " + task.getException());
//                        }
//                    } finally {
//                        loadingDialog.hide();
//                    }
//                });
//    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Step 1: Get Firebase credentials from Google
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // Step 2: Sign in with Firebase using credentials
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // If sign-in is successful, get the FirebaseUser object
                FirebaseUser user = mAuth.getCurrentUser();
                assert user != null;
                String uid = user.getUid();
                String googleEmail = user.getEmail();
                String displayName = user.getDisplayName();

                // Split display name into first and last names (if possible)
                String firstName = (displayName != null && displayName.contains(" "))
                        ? displayName.split(" ")[0] : displayName;
                String lastName = (displayName != null && displayName.contains(" "))
                        ? displayName.split(" ")[1] : "";

                // Retrieve the profile picture URL from the FirebaseUser object
                Uri photoUrl = user.getPhotoUrl();
                String profilePictureUrl = (photoUrl != null) ? photoUrl.toString() : null;

                // Logging details for debugging
                Log.d(TAG, "First Name: " + firstName);
                Log.d(TAG, "Last Name: " + lastName);
                Log.d(TAG, "Google Email: " + googleEmail);
                Log.d(TAG, "Profile Picture URL: " + profilePictureUrl);

                // Save user data to Firestore including the profile picture URL
                saveUserToFirestore(uid, firstName, lastName, googleEmail, null, profilePictureUrl);
                createDialoge(LoginActivity.this, true); // Show new user dialog
                Log.d(TAG, "New user registration process completed.");
            } else {
                // Handle sign-in failure
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Utill.showToast(LoginActivity.this, "Authentication Failed: " + task.getException());
            }
            loadingDialog.hide();
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
                createDialoge(LoginActivity.this, true);
            } else {
                Utill.showToast(this, "User Not Found : " + task.getException());
            }
            loadingDialog.hide();
        });
    }


    private void signupuser(EditText fname, EditText lname, EditText emai, EditText pass, EditText confirmpassword) {

        String firstname = fname.getText().toString();
        String lastname = lname.getText().toString();
        String email = emai.getText().toString();
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

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(fname + " " + lname).build();

                user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Utill.showToast(this, "User Profile Updated");
                        saveUserToFirestore(user.getUid(), fname, lname, email, password, defaultImageURL);
                        createDialoge(LoginActivity.this, false);
                    } else {
                        Utill.showToast(this, "User Profile Update Failed");
                    }
                    loadingDialog.hide();
                });

            } else {
                Utill.showToast(this, "Signup Failed : " + task.getException());
                loadingDialog.hide();
            }
        });
    }


    private void saveUserToFirestore(String uid, String fname, String lname, String email, String password, String profilePictureUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map to store user details
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fname", fname);
        user.put("lname", lname);
        user.put("email", email);
        user.put("password", password); // Consider not storing passwords in Firestore for security reasons
        user.put("profile_picture", profilePictureUrl); // New field for profile picture URL
        user.put("upi", null); // Set initial UPI as null

        // Generate search keywords from first and last names
        List<String> searchKeywords = new ArrayList<>();
        searchKeywords.addAll(generateSearchKeywords(fname));
        searchKeywords.addAll(generateSearchKeywords(lname));
        user.put("searchKeywords", searchKeywords);

        // Save user details to Firestore under the "users" collection
        db.collection("users")
                .document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User saved to Firestore with search keywords.");
                    } else {
                        Log.e(TAG, "Error saving user: ", task.getException());
                    }
                    // Hide your loading dialog or perform further actions here
                    loadingDialog.hide();
                });
    }


    private void createDialoge(Activity activity, Boolean islogin) {
        loadingDialog.hide();
        // Use the Activity context
        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(activity).inflate(R.layout.loginsuccess, null);

        // Create a dialog
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(dialogView);

        // Optional: Make the dialog background transparent or set it to a specific background color
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView titleText = dialogView.findViewById(R.id.textView1);
        TextView description = dialogView.findViewById(R.id.textView2);

        if (Boolean.TRUE.equals(islogin)) {
            titleText.setText(R.string.loginsuccesstitle);
            description.setText(R.string.loginsuccessdesc);
        } else {
            titleText.setText(R.string.regestrationtitle);
            description.setText(R.string.regsuccessdesc);
        }

        // Optional: Set the dialog to cancel on outside touch
        dialog.setCancelable(false);

        // Show the dialog
        dialog.show();

        new Handler().postDelayed(() -> navigateToHomepage(activity), 3500);

    }

    private void navigateToHomepage(Activity activity) {
        // Transition to HomeActivity after 4 seconds
        Intent intent = new Intent(activity, Homepage.class);
        startActivity(intent);
        // Finish the DialogActivity so it’s no longer on the back stack
        finish();
    }

    //    this method will generate the search keyword for each user...
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

}