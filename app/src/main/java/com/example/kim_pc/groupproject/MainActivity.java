package com.example.kim_pc.groupproject;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.CheckResult;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.kim_pc.groupproject.Model.CheckUserResponse;
import com.example.kim_pc.groupproject.Model.User;
import com.example.kim_pc.groupproject.Retrofit.MovieShopAPI;
import com.example.kim_pc.groupproject.Utils.Common;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1000;
    Button btn_next;

    MovieShopAPI mSer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSer = Common.getAPI();

        btn_next = (Button)findViewById(R.id.btn_next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startLoginPage(LoginType.PHONE);

            }
        });


    }

    private void startLoginPage(LoginType loginType) {
        Intent intent = new Intent(this,AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType,
                        AccountKitActivity.ResponseType.TOKEN);

        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,builder.build());
        startActivityForResult(intent,REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE)
        {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

            if(result.getError() != null)
            {
                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
            }
            else if(result.wasCancelled())
            {
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
            }
            else
            {
                if(result.getAccessToken() != null)
                {
                    final android.app.AlertDialog alertDialog = new SpotsDialog(MainActivity.this);
                    alertDialog.show();
                    alertDialog.setMessage("Please waite");


                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {

                            mSer.checkUserExists(account.getPhoneNumber().toString())
                                    .enqueue(new Callback<CheckUserResponse>() {
                                        @Override
                                        public void onResponse(Call<CheckUserResponse> call, Response<CheckUserResponse> response) {
                                            CheckUserResponse userResponse = response.body();
                                            if(userResponse.isExists())
                                            {
                                                alertDialog.dismiss();
                                            }
                                            else
                                            {
                                               alertDialog.dismiss();

                                               showRegisterDialog(account.getPhoneNumber().toString());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CheckUserResponse> call, Throwable t) {

                                        }
                                    });

                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                            Log.d("ERROR",accountKitError.getErrorType().getMessage());

                        }
                    });
                }
            }
        }
    }

    private void showRegisterDialog(final String phone) {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("REGISTER");

        LayoutInflater inflater = this.getLayoutInflater();
        View register = inflater.inflate(R.layout.register,null);

        final MaterialEditText edt_name = (MaterialEditText) register.findViewById(R.id.edt_name);
        final MaterialEditText edt_address = (MaterialEditText) register.findViewById(R.id.edt_address);
        final MaterialEditText edt_birthdate = (MaterialEditText) register.findViewById(R.id.edt_birthdate);

        Button btn_register = (Button) register.findViewById(R.id.btn_register);

        edt_birthdate.addTextChangedListener(new PatternedTextWatcher("####-##-##"));

        btn_register.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                alertDialog.create().dismiss();

                final android.app.AlertDialog waitDialog = new SpotsDialog(MainActivity.this);
                waitDialog.show();
                waitDialog.setMessage("Please waite");

                if(TextUtils.isEmpty(edt_address.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please enter the address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edt_birthdate.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please enter the birthdate", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edt_name.getText().toString()))
                {
                    Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                final android.app.AlertDialog alertDialog = new SpotsDialog(MainActivity.this);
                alertDialog.show();
                alertDialog.setMessage("Please waite");


                mSer.registerNewUser(phone,
                        edt_name.getText().toString(),
                        edt_address.getText().toString(),
                        edt_birthdate.getText().toString())
                        .enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> call, Response<User> response) {
                                waitDialog.dismiss();
                                User user = response.body();

                                if(TextUtils.isEmpty(user.getError_msg()))
                                {
                                    Toast.makeText(MainActivity.this, "User register success", Toast.LENGTH_SHORT).show();
                                }

                            }

                            @Override
                            public void onFailure(Call<User> call, Throwable t) {
                                waitDialog.dismiss();
                            }
                        });


            }

        });

        alertDialog.setView(register);
        alertDialog.show();

    }

    private void printKeyHash() {
        try{
            PackageInfo info = getPackageManager().getPackageInfo("com.example.kim_pc.groupproject",
                    PackageManager.GET_SIGNATURES);

            for(Signature Signature:info.signatures)
            {
                MessageDigest msg = MessageDigest.getInstance("SHA");
                msg.update(Signature.toByteArray());
                Log.d("KEYHASH", Base64.encodeToString(msg.digest(),Base64.DEFAULT));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
