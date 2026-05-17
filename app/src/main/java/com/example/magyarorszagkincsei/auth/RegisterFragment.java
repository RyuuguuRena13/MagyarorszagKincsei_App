package com.example.magyarorszagkincsei.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.magyarorszagkincsei.main.MainActivity;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.auth.FirebaseAuth;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button registerBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        emailInput = view.findViewById(R.id.regEmail);
        passwordInput = view.findViewById(R.id.regPassword);
        registerBtn = view.findViewById(R.id.registerBtn);

        registerBtn.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("E-mail cím megadása kötelező!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Jelszó megadása kötelező!");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("A jelszónak legalább 6 karakterből kell állnia!");
            return;
        }

        registerBtn.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    registerBtn.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        requireActivity().finish();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Ismeretlen hiba";
                        Toast.makeText(getContext(), "Hiba: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}