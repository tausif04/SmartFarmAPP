package com.iot.smartFarming;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class SignUP extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 10;
    private static final int UCROP_REQUEST_CODE = UCrop.REQUEST_CROP;

    private Button signup;
    private EditText username, email, password, retypePassword;
    private ImageView profilePic;
    private Uri imageUri;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        signup = findViewById(R.id.signup_button);
        username = findViewById(R.id.username);
        email = findViewById(R.id.login_email);
        password = findViewById(R.id.Password);
        retypePassword = findViewById(R.id.retypePassword);
        profilePic = findViewById(R.id.profile_pic);
        findViewById(R.id.login_txt).setOnClickListener(v -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void setupClickListeners() {
        signup.setOnClickListener(v -> attemptRegistration());
        profilePic.setOnClickListener(v -> openImagePicker());
    }

    private void attemptRegistration() {
        String name = username.getText().toString().trim();
        String emailStr = email.getText().toString().trim().toLowerCase();
        String pass = password.getText().toString().trim();
        String cPass = retypePassword.getText().toString().trim();

        if (!validateInputs(name, emailStr, pass, cPass)) return;

        auth.createUserWithEmailAndPassword(emailStr, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendVerificationEmail();
                        saveUserData(task.getResult().getUser().getUid(), name, emailStr, pass);
                    } else {
                        handleRegistrationError(task.getException().getMessage());
                    }
                });
    }

    private boolean validateInputs(String name, String email, String pass, String cPass) {
        if (TextUtils.isEmpty(name)) {
            username.setError("Name required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            this.email.setError("Email required");
            return false;
        }
        if (pass.length() < 8) {
            password.setError("Minimum 8 characters");
            return false;
        }
        if (!pass.equals(cPass)) {
            retypePassword.setError("Passwords don't match");
            return false;
        }
        return true;
    }

    private void sendVerificationEmail() {
        auth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String userId, String name, String email, String password) {
        DatabaseReference userRef = database.getReference("Users").child(userId);
        StorageReference storageRef = storage.getReference("Uploads").child(userId);

        if (imageUri != null) {
            storageRef.putFile(imageUri)
                    .continueWithTask(task -> storageRef.getDownloadUrl())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String imageUrl = task.getResult().toString();
                            createUser(userRef, userId, name, email, imageUrl);
                        } else {
                            createUser(userRef, userId, name, email, getDefaultAvatar());
                        }
                    });
        } else {
            createUser(userRef, userId, name, email, getDefaultAvatar());
        }
    }

    private void createUser(DatabaseReference userRef, String userId, String name,
                            String email, String imageUrl) {
        User user = new User(userId, name, email, imageUrl, "Hey I'm using this app");
        userRef.setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        auth.signOut();
                        startActivity(new Intent(this, Login.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getDefaultAvatar() {
        return "https://firebasestorage.googleapis.com/v0/b/cheer-park-93e89.appspot.com/o/man.png?alt=media&token=e8a2bc7e-94c8-4be5-994f-d6114f3c8453";
    }

    private void handleRegistrationError(String error) {
        if (error.contains("email address is already")) {
            email.setError("Email already registered");
        } else if (error.contains("badly formatted")) {
            email.setError("Invalid email format");
        } else {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped.jpg"));

            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(500, 500)
                    .start(this);
        } else if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK) {
            imageUri = UCrop.getOutput(data);
            Glide.with(this)
                    .load(imageUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(profilePic);
        }
    }

    public static class User {
        public String id, name, email, profile, status;

        public User() {}

        public User(String id, String name, String email, String profile, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.profile = profile;
            this.status = status;
        }
    }
}