package com.example.bruno.publicador;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements LoginDialog.NoticeDialogListener {

    Button make;
    Button uploadPicture;
    Button login;
    ImageView adminPicture;
    FirebaseLogin mFirebaseLogin;
    FirebaseBD mFirebaseBD;
    FirebaseUser fUser;
    private final int MY_PERMISSIONS = 100;
    private CoordinatorLayout mRlView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Generamos los botones principales
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRlView = (CoordinatorLayout) findViewById(R.id.cl_view);

        mFirebaseLogin = new FirebaseLogin();
        mFirebaseBD = new FirebaseBD();

        //Boton para subir fotos a firebase
        uploadPicture = (Button) findViewById(R.id.loadPicture);
        uploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,  LoadPictureActivity.class);
                startActivity(intent);
            }
        });

        login = (Button) findViewById(R.id.buttonLogin);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialogInputData();
            }
        });

        adminPicture = (ImageView) findViewById(R.id.imageUser);

        if(mayRequestStoragePermission())
            uploadPicture.setEnabled(true);
        else
            uploadPicture.setEnabled(false);

        if (mFirebaseLogin.checkUserStatus()){
            fUser = mFirebaseLogin.getUser();
            cargarUserPictureEnView();
        }
        else
            Toast.makeText(MainActivity.this, "Usuario deslogueado", Toast.LENGTH_SHORT).show();



    }

    private boolean mayRequestStoragePermission() {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        if((checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED))
            return true;

        if((shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) || (shouldShowRequestPermissionRationale(CAMERA))){
            Snackbar.make(mRlView, "Los permisos son necesarios para poder usar la aplicación",
                    Snackbar.LENGTH_INDEFINITE).setAction(android.R.string.ok, new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(View v) {
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, MY_PERMISSIONS);
                }
            });
        }else{
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, MY_PERMISSIONS);
        }

        return false;
    }


    private void openDialogInputData() {
        DialogFragment loginDialog = new LoginDialog();
        loginDialog.show(getFragmentManager(), "loginDialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseLogin.addAuthListenerOnStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseLogin.removeAuthListenerOnStop();
    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String userName, String password) {
        //mFirebaseLogin.connect(MainActivity.this, userName, password);
       doitBackgroundTask(userName,password);
    }

    public void doitBackgroundTask(String username, String password){
        BackgroundTask task = null;
        try {
            task = new BackgroundTask(MainActivity.this, username, password);
            task.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*TAREA ASINCRONA DE LOGIN  */
    private class BackgroundTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        String userName;
        String password;

        public BackgroundTask(MainActivity activity, String userName, String password) throws IOException {
            dialog = new ProgressDialog(activity);
            this.userName = userName;
            this.password = password;
            cargarUserPictureEnView();
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Ingresando Al sistema");
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            fUser = mFirebaseLogin.getUser();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mFirebaseLogin.connect(MainActivity.this, userName, password);

            return null;
        }

    }


    /*FIN DE TAREA ASINCRONA DE LOGIN*/


    private void cargarUserPictureEnView() {
        try {
            mFirebaseBD.downloadUserPicture(adminPicture);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == MY_PERMISSIONS){
            if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "Permisos aceptados", Toast.LENGTH_SHORT).show();
                uploadPicture.setEnabled(true);
            }
        }else{
            showExplanation();
        }
    }

    private void showExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Permisos denegados");
        builder.setMessage("Para usar las funciones de la app necesitas aceptar los permisos");
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }
}



