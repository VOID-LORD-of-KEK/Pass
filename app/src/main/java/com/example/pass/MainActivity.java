package com.example.pass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.pass.db.AppDatabase;
import com.example.pass.db.User;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_NAME = "KeyName";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String FORWARD_SLASH = "/";
    //Генерируем ключ
    private void generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
    keyGenerator.init(keyGenParameterSpec);
    keyGenerator.generateKey();
}
    //Получаем ключ
    private SecretKey getSecretKey() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return ((SecretKey)keyStore.getKey(KEY_NAME, null));
    }
    //получаем шифр
    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + FORWARD_SLASH + KeyProperties.BLOCK_MODE_CBC + FORWARD_SLASH + KeyProperties.ENCRYPTION_PADDING_PKCS7);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
        Ситуация такова: мне нужно получить список зашифрованнных паролей. Если в этом списке есть запись зашифрованного пароля для моего ключа,
        то вызываю окно с биометрией(условно это другой biometricprompt). ИНаче- окно с ПИном.
        Единственное, что мне нужно сделать: понять как вызвать этот список и правильно написать условие. А создание окна для биометрии и ПИН- я сам смогу.
         */


        //создаем связь с кнопками, виджетами и т. п. из activity_main.xml
        TextView username = findViewById(R.id.username);
        TextView password = findViewById(R.id.password);
        CheckBox checkBox = findViewById(R.id.check);
        MaterialButton EnterButton = findViewById(R.id.EnterButton);

        //создаем биометрикменеджер, который определит работу биометрии в устройстве с экзекутором
        BiometricManager manager = BiometricManager.from(this);
        Executor executor = ContextCompat.getMainExecutor(this);
        if(manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                ==BiometricManager.BIOMETRIC_SUCCESS){
            checkBox.setVisibility(View.VISIBLE);
            try {
                generateSecretKey(new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .setInvalidatedByBiometricEnrollment(true)
                        .build());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        } 
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this,"Auth error",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                byte[] encryptedInfo = new byte[0];
                try {
                    encryptedInfo = result.getCryptoObject().getCipher().doFinal(password.toString().getBytes(Charset.defaultCharset()));
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }


                saveNewUser(username.getText().toString(),encryptedInfo.toString());
                startActivity(new Intent(MainActivity.this, EmptyActivity.class));
                Toast.makeText(MainActivity.this,"Auth success and "+ encryptedInfo,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this,"Auth failed",Toast.LENGTH_SHORT).show();
            }
        });



        EnterButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if (username.getText().toString().equals("admin") && password.getText().toString().equals("admin")) {
                    if(checkBox.isChecked()){
                        if(manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                ==BiometricManager.BIOMETRIC_SUCCESS){

                            Cipher cipher = null;
                            try {
                                cipher = getCipher();
                            } catch (NoSuchPaddingException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                            SecretKey secretKey = null;
                            try {
                                secretKey = getSecretKey();
                            } catch (KeyStoreException e) {
                                e.printStackTrace();
                            } catch (CertificateException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (UnrecoverableKeyException e) {
                                e.printStackTrace();
                            }
                            try {
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            }
                            /*
                            Cipher cipher1 = null;
                            try {
                                cipher1 = getCipher();
                            } catch (NoSuchPaddingException e) {
                                e.printStackTrace();
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }

                            try{
                                cipher1.init(Cipher.DECRYPT_MODE,secretKey);
                            } catch (InvalidKeyException e) {
                                e.printStackTrace();
                            }

                             */

                            //вызываем окно для биометрии с криптоданными
                            BiometricPrompt.PromptInfo.Builder promptInfo = dialogMetric();
                            promptInfo.setNegativeButtonText("Cancel");
                            biometricPrompt.authenticate(promptInfo.build(),new BiometricPrompt.CryptoObject(cipher));
                        } else{
                            Toast.makeText(MainActivity.this, "NO BIO", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, EmptyActivity.class));
                        }
                    } else{
                        Toast.makeText(MainActivity.this, "DO NOT CHOOSE BIO", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, EmptyActivity.class));
                    }
                } else {
                    Toast.makeText(MainActivity.this, "LOGIN FAIL", Toast.LENGTH_SHORT).show();
                    password.setText("");
                }
            }
        });

    }

    BiometricPrompt.PromptInfo.Builder dialogMetric()
    {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Login using your biometric credential");

    }
    private void saveNewUser(String UserName, String PassWord) {
        AppDatabase db  = AppDatabase.getdbInstance(this.getApplicationContext());

        User user = new User();
        user.username = UserName;
        user.password = PassWord;
        db.userDao().insertUser(user);

        finish();

    }
}

