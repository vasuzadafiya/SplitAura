package com.example.splitaura;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    //    for sign-in reference variables
    EditText fname, lname, email, password, confirmpassword;
    Button createac;
    private FirebaseAuth mAuth;
    TextView signup;
    TextView login;

    //    for Login Credential
    EditText lemail;
    EditText lpassword;
    Button loginBtn;

    //    for custom LoginDialogue
    private LoadingDialog loadingDialog;

    ConstraintLayout signupConstraint;
    ConstraintLayout loginConstraint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

//        intialize signup variable
        fname = findViewById(R.id.fname_edit_text);
        lname = findViewById(R.id.lanme_edit_text);
        email = findViewById(R.id.email1_edit_text);
        password = findViewById(R.id.password1_edit_text);
        createac = findViewById(R.id.create_acc_button);
        confirmpassword = findViewById(R.id.confirm_password1_edit_text);
        mAuth = FirebaseAuth.getInstance();

//        intialize Login variables
        lemail = findViewById(R.id.email_edit_text);
        lpassword = findViewById(R.id.password_edit_text);
        loginBtn = findViewById(R.id.login_button);
        signupConstraint = findViewById(R.id.signin_constarint);
        loginConstraint = findViewById(R.id.login_constarint);
        loadingDialog = new LoadingDialog(this);


        createac.setOnClickListener(view -> signupuser(fname, lname, email, password, confirmpassword));

        loginBtn.setOnClickListener(view -> loginuser(lemail, lpassword));

        signup = findViewById(R.id.sign_up);
        signup.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.VISIBLE);
            loginConstraint.setVisibility(View.GONE);
        });

        login = findViewById(R.id.log_in);
        login.setOnClickListener(view -> {
            signupConstraint.setVisibility(View.GONE);
            loginConstraint.setVisibility(View.VISIBLE);
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
                navigateToHomepage(LoginActivity.this, true);
            } else {
                Utill.showToast(this, "Login Failed : " + task.getException());
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
            signUpUser(firstname, lastname, email, password);
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


    private void signUpUser(String fname, String lname, String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {

            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(fname + " " + lname).build();

                user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Log.d("Signup", "User Profile Updated");
                        Utill.showToast(this, "User Profile Updated");
                        saveUserToFirestore(user.getUid(), fname, lname, email, password);
                    } else {
                        Log.e("Signup", "User Profile Update Failed");
                        Utill.showToast(this, "User Profile Update Failed");
                    }
                    loadingDialog.hide();
                });

            } else {
                Log.e("Signup", "Signup Failed : " + task.getException());
                Utill.showToast(this, "Signup Failed : " + task.getException());
                loadingDialog.hide();
            }
        });
    }

    //    for save new user to firebase database
    private void saveUserToFirestore(String uid, String fname, String lname, String email, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map to store user details
        Map<String, String> user = new HashMap<>();
        user.put("uid", uid);
        user.put("fname", fname);
        user.put("lname", lname);
        user.put("email", email);
        user.put("password", password);
        user.put("upi", null); // Set initial UPI as null

        // Save to Firestore
        db.collection("users")
                .document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Utill.showToast(this, "User saved to Firestore");
//
                        navigateToHomepage(LoginActivity.this, false);
                    } else {
                        Utill.showToast(this, "Failed to save user to Firestore");
                    }
                })
                .addOnFailureListener(e -> Utill.showToast(this, "Error: " + e.getMessage()));
        loadingDialog.hide();
    }

    private void navigateToHomepage(Activity activity, Boolean isLogin) {
        loadingDialog.hide();
        // Use the Activity context
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.loginsuccess,null);

        // Create a dialog
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(dialogView);

        // Optional: Make the dialog background transparent or set it to a specific background color
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView titleText = dialogView.findViewById(R.id.textView1);
        TextView description = dialogView.findViewById(R.id.textView2);

        if (Boolean.TRUE.equals(isLogin)) {
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
        new Handler().postDelayed(() -> {
            // Transition to HomeActivity after 4 seconds
            Intent intent = new Intent(activity, Homepage.class);
            startActivity(intent);
            // Finish the DialogActivity so it’s no longer on the back stack
            finish();
        }, 3500);
    }

}