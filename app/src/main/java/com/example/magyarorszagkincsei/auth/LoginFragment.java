package com.example.magyarorszagkincsei.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.magyarorszagkincsei.main.MainActivity;
import com.example.magyarorszagkincsei.R; // HOZZÁADVA
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button loginBtn, guestBtn;
    private TextView goToReg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View v) {
        emailInput = v.findViewById(R.id.emailInput);
        passwordInput = v.findViewById(R.id.passwordInput);
        loginBtn = v.findViewById(R.id.loginButton);
        guestBtn = v.findViewById(R.id.guestButton);
        goToReg = v.findViewById(R.id.goToRegister);
    }

    private void setupClickListeners() {
        loginBtn.setOnClickListener(v -> handleLogin());
        guestBtn.setOnClickListener(v -> navigalMainre());
        goToReg.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_login_to_register));
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Az e-mail cím megadása kötelező!");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("A jelszó megadása kötelező!");
            return;
        }

        loginBtn.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    loginBtn.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Sikeres bejelentkezés!", Toast.LENGTH_SHORT).show();
                        navigalMainre();
                    } else {
                        String hibaUzenet = task.getException() != null ? task.getException().getMessage() : "Ismeretlen hiba";
                        Toast.makeText(getContext(), "Hiba: " + hibaUzenet, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigalMainre() {
        startActivity(new Intent(getActivity(), MainActivity.class));
        requireActivity().finish();
    }
}