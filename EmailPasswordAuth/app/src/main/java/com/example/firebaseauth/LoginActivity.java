package com.example.firebaseauth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    EditText et_username, et_password;
    Button btn_login, btn_signup, btn_google_signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Authentication and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Configure Google Sign-In
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        // Bind UI elements
        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        btn_login = findViewById(R.id.btn_login);
        btn_signup = findViewById(R.id.btn_already_have_account);
        btn_google_signIn = findViewById(R.id.btn_google_signIn);

        // Handle login button click
        btn_login.setOnClickListener(v -> {
            String input = et_username.getText().toString();
            String password = et_password.getText().toString();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter username/email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                loginWithEmail(input, password);
            } else {
                mDatabase.getReference("Users")
                        .orderByChild("username")
                        .equalTo(input)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        User user = snapshot.getValue(User.class);
                                        if (user != null) {
                                            loginWithEmail(user.email, password);
                                        }
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // Handle "Sign Up" button click
        btn_signup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Handle Google Sign-In button click
        btn_google_signIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent googleSignInIntent = gsc.getSignInIntent();
        startActivityForResult(googleSignInIntent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Sign in failed: " + e.getStatusCode());
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserExists(user);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserExists(FirebaseUser user) {
        String uid = user.getUid();
        mDatabase.getReference().child("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        } else {
                            promptForUsername(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void promptForUsername(FirebaseUser user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter a unique username");

        final EditText input = new EditText(this);
        input.setHint("Username");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String username = input.getText().toString().trim();
            if (!username.isEmpty()) {
                checkUsernameAvailability(user, username);
            } else {
                Toast.makeText(LoginActivity.this, "Username cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void checkUsernameAvailability(FirebaseUser user, String username) {
        mDatabase.getReference().child("Users")
                .orderByChild("username")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Toast.makeText(LoginActivity.this, "Username already taken. Please try another.", Toast.LENGTH_SHORT).show();
                            promptForUsername(user);
                        } else {
                            createNewUser(user, username);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewUser(FirebaseUser user, String username) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("email", user.getEmail());
        userMap.put("username", username);
        userMap.put("profileImage", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userMap.put("uid", user.getUid());

        mDatabase.getReference().child("Users").child(user.getUid()).setValue(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Account created successfully.", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to create account. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}