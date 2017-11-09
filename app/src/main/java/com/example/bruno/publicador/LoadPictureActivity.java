package com.example.bruno.publicador;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission_group.CAMERA;

public class LoadPictureActivity extends AppCompatActivity {
    private static final int SELECT_MULTIPLE_PICTURE = 400;

    // private static String APP_DIRECTORY = "CarnesLaColorada/";
   //  private static String MEDIA_DIRECTORY = APP_DIRECTORY + "Picture";

    private final int PHOTO_CODE = 200;
    private static final int SELECT_PICTURE = 300 ;
    private ImageView foto;
    Button buttonGalerySelect;
    Button buttonUploadFirebase;
    Button buttonMultipleSelection;
    FloatingActionButton buttonTakePicture;
    public FirebaseBD firebaseBD;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_picture);

        buttonGalerySelect  = (Button) findViewById(R.id.chargeGalleryButton);
        buttonTakePicture = (FloatingActionButton) findViewById(R.id.fotoActionButton);
        buttonMultipleSelection = (Button) findViewById(R.id.buttonSelectionMultiple);
        buttonUploadFirebase = (Button) findViewById(R.id.uploadFirebaseButton);
        foto = (ImageView) findViewById(R.id.imageToUp);
        firebaseBD = new FirebaseBD();


        buttonGalerySelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Selecciona app de imagen"), SELECT_PICTURE);

            }
        });

        buttonMultipleSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Selecciona app de imagen"), SELECT_MULTIPLE_PICTURE);
            }
        });


        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();

            }
        });

        buttonUploadFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseBD.uploadPictureToFirebaseByBitmap(foto);
            }
        });



    }






    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, PHOTO_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri selectedImageUri = null;
        Uri selectedImage;
        String s;


        if(resultCode == RESULT_OK){
            switch (requestCode){
                case PHOTO_CODE:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    foto.setImageBitmap(imageBitmap);

                    break;
                case SELECT_PICTURE:
                    selectedImage = data.getData();
                    Bitmap bitmap;
                    try {
                         bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                         Bitmap newBitmap = redimensionarImagenMaximo(bitmap,foto.getWidth(),foto.getHeight());
                         foto.setImageBitmap(newBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                   // foto.setImageURI(selectedImage);
                    /*String selectedPath = selectedImage.getPath();
                        if (selectedPath != null) {
                            InputStream imageStream = null;
                            setPic(selectedPath);

                           // Transformamos la URI de la imagen a inputStream y este a un Bitmap
                            Bitmap bmp = BitmapFactory.decodeStream(imageStream);

                            // Ponemos nuestro bitmap en un ImageView que tengamos en la vista
                            foto.setImageBitmap(bmp);
                        }*/
                    break;
                case SELECT_MULTIPLE_PICTURE:
                    ClipData clipData = data.getClipData();
                    if(clipData != null) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            uploadImageToFirebase(uri) ;
                        }
                    }

                    break;
            }
        }else {
            Toast.makeText(LoadPictureActivity.this, "Hubo un error al cargar la foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(Uri uri) {
    firebaseBD.uploadPictureByUri(uri);
    }


    private void setPic(String selectedPath) {
        // Get the dimensions of the View
        int targetW = foto.getWidth();
        int targetH = foto.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(selectedPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(selectedPath, bmOptions);
        foto.setImageBitmap(bitmap);
    }


    public Bitmap redimensionarImagenMaximo(Bitmap mBitmap, float newWidth, float newHeigth){
        //Redimensionamos
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeigth) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, false);
    }
}
