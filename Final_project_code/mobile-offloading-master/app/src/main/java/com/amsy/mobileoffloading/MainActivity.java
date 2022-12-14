package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import android.content.Intent;
import android.widget.Toast;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;



public class MainActivity extends AppCompatActivity {
    private final int CODE_PERMISSION_SEEK = 111;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onRequestPermissionsResult(int codeForRequest, @NonNull String[] permits, @NonNull int[] resultOfRequestPermissions) {
        super.onRequestPermissionsResult(codeForRequest, permits, resultOfRequestPermissions);

        if (codeForRequest == CODE_PERMISSION_SEEK) {
            for (int resultOfRequestPermission : resultOfRequestPermissions) {
                if (resultOfRequestPermission == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(getApplicationContext(), "Allow required permissions", Toast.LENGTH_LONG).show();
                    onBackPressed();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] permissionsNeeded =  checkPermissions();
        if (permissionsNeeded.length > 0) {
            requestForPermits(permissionsNeeded);
        }
    }


    private String[] checkPermissions() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();
        try {
            String nameOfPackage = getPackageName();
            PackageInfo detailsOfPackage = getPackageManager().getPackageInfo(nameOfPackage, PackageManager.GET_PERMISSIONS);
            String[] permits = detailsOfPackage.requestedPermissions;

            for (String permit : permits) {
                if (ActivityCompat.checkSelfPermission(this, permit) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(permit);
                }
            }
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }

        String[] _permissionsNeeded = new String[permissionsNeeded.size()];
        _permissionsNeeded = permissionsNeeded.toArray(_permissionsNeeded);
        return _permissionsNeeded;
    }


    public void onClickModeSlave(View view) {
        Intent intent = new Intent(getApplicationContext(), WorkerAvailableNotice.class);
        startActivity(intent);
    }


    private void requestForPermits(String[] permissionsNeeded) {
        ActivityCompat.requestPermissions(this, permissionsNeeded, CODE_PERMISSION_SEEK);
    }

    public void onClickModeMaster(View view) {
        Intent intent = new Intent(getApplicationContext(), WorkersSearch.class);
        startActivity(intent);
    }
}