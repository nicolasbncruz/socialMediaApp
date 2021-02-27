package com.ncruzc.socialmediagamer.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ncruzc.socialmediagamer.R;
import com.ncruzc.socialmediagamer.providers.AuthProvider;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView mTextViewRegister;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputPassword;
    Button mButtonLogin;
    SignInButton mButtonGoogle;
    AuthProvider mAuthProvider;
    private GoogleSignInClient mGoogleSignInClient;
    private final int REQUEST_CODE_GOOGLE = 1;
    FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewRegister = findViewById(R.id.textViewRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputPassword = findViewById(R.id.textInputPassword);
        mButtonLogin = findViewById(R.id.btnLogin);
        mButtonGoogle = findViewById(R.id.btnLoginGoogle);

        mAuthProvider = new AuthProvider();
        //se crea un objeto con todas las caracteristicas necesarias para la autenticacion con google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();//inicializando
        //vamos a inicializar el objeto googleSignInClient
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mFirestore = FirebaseFirestore.getInstance();

        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        mButtonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        mTextViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    //metodo que nos permite disparar la autenticación
    private void signInWithGoogle(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_CODE_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("ERROR", "Google sign in failed", e);
            }
        }
    }
    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        mAuthProvider.googleLogin(account)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String id = mAuthProvider.getUid();
                            checkUserExist(id);

                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("ERROR", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "No se pudo iniciar sesión con google", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkUserExist(final String id) {
        mFirestore.collection("Users").document().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }else {

                    String email = mAuthProvider.getEmail();
                    Map<String, Object> map = new HashMap<>();
                    map.put("email", email);
                    mFirestore.collection("Users").document(id).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Intent intent = new Intent(MainActivity.this, CompleteProfileActivity.class);
                                startActivity(intent);
                            }else {
                                Toast.makeText(MainActivity.this, "No se pudo almacenar la infornación del usuario", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }


    private void login(){
        String email = mTextInputEmail.getText().toString();
        String password = mTextInputPassword.getText().toString();
        mAuthProvider.login(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, "El email o la contraseña que ingresaste no son correctas", Toast.LENGTH_LONG).show();
                }
            }
        });
        Log.d("CAMPO", "email: "+email);
        Log.d("CAMPO", "password: "+password);
    }

}
