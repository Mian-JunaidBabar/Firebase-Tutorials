package com.example.firebase_curd_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebase_curd_app.adapters.UserAdapter;
import com.example.firebase_curd_app.models.UserModel;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etEmail, etAge;
    private Button btnSave;
    private RecyclerView recyclerView;

    private DatabaseReference databaseReference;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("users");

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etAge = findViewById(R.id.etAge);
        btnSave = findViewById(R.id.btnSave);
        recyclerView = findViewById(R.id.recyclerView);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = databaseReference.orderByKey();

        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class)
                .build();

        userAdapter = new UserAdapter(options);
        recyclerView.setAdapter(userAdapter);

        // Set click listener for adapter items
        userAdapter.setOnItemClickListener(new UserAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(com.google.firebase.database.DataSnapshot snapshot, int position) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    etName.setText(user.getName());
                    etEmail.setText(user.getEmail());
                    etAge.setText(String.valueOf(user.getAge()));
                    btnSave.setText("Update");

                    // Store the current user ID for update
                    btnSave.setTag(user.getId());
                }
            }

            @Override
            public void onDeleteClick(com.google.firebase.database.DataSnapshot snapshot, int position) {
                String userId = snapshot.getKey();
                if (userId != null) {
                    databaseReference.child(userId).removeValue();
                    Toast.makeText(MainActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Save/Update button click listener
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int age = Integer.parseInt(ageStr);

            if (btnSave.getText().toString().equals("Update")) {
                // Update existing user
                String userId = (String) btnSave.getTag();
                if (userId != null) {
                    UserModel user = new UserModel(userId, name, email, age);
                    databaseReference.child(userId).setValue(user);
                    Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Create new user
                String userId = databaseReference.push().getKey();
                if (userId != null) {
                    UserModel user = new UserModel(userId, name, email, age);
                    databaseReference.child(userId).setValue(user);
                    Toast.makeText(this, "User saved", Toast.LENGTH_SHORT).show();
                }
            }

            // Clear fields and reset button
            clearFields();
        });
    }

    private void clearFields() {
        etName.setText("");
        etEmail.setText("");
        etAge.setText("");
        btnSave.setText("Save");
        btnSave.setTag(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        userAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        userAdapter.stopListening();
    }
}