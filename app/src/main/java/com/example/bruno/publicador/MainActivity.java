package com.example.bruno.publicador;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LoginDialog.NoticeDialogListener {

    Button make;
    Button uploadPicture;
    Button login;
    ImageView adminPicture;
    FirebaseLogin mFirebaseLogin;
    FirebaseBD mFirebaseBD;
    FirebaseUser fUser;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Generamos los botones principales
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        BackgroundTask task = null;
        try {
            task = new BackgroundTask(MainActivity.this, userName, password);
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
}



