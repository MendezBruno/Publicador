package com.example.bruno.publicador;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bruno on 27/10/2017.
 */
public class FirebaseBD {
    private StorageReference mStorageRef;
    private FirebaseDatabase database;
    private static String FIREBASE_DIRECTORY_REFERENCE = "imagenes";
    private static String SEPARADOR = "/";

    FirebaseBD (){
       setmStorageRef(FirebaseStorage.getInstance().getReference());
        setmDatabaseInstance( FirebaseDatabase.getInstance());
    }

    private void setmDatabaseInstance(FirebaseDatabase instance) { this.database = instance; };
    public FirebaseDatabase getmDatabaseInstance () { return database; }


    public StorageReference getmStorageRef() {
        return mStorageRef;
    }

    private StorageReference getUploadReference(){
        return getmStorageRef().child(FIREBASE_DIRECTORY_REFERENCE+SEPARADOR+imageName());
    }

    private void setmStorageRef(StorageReference mStorageRef) {
        this.mStorageRef = mStorageRef;
    }

    void uploadPictureToFirebaseByBitmap(ImageView foto) {
        final StorageReference uploadReference = getUploadReference();
        foto.setDrawingCacheEnabled(true);
        foto.buildDrawingCache();
        Bitmap bitmap = foto.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = uploadReference.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                saveUriAndName(downloadUrl, uploadReference.getPath());
            }
        });
    }

    void uploadPictureToFirebaseByStream(ImageView foto) {

        foto.setDrawingCacheEnabled(true);
        foto.buildDrawingCache();
        Bitmap bitmap = foto.getDrawingCache();
        InputStream stream = bitmapToInputStream(bitmap);
        UploadTask uploadTask = mStorageRef.putStream(stream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();

            }
        });
    }

    void uploadPictureByUri(Uri file){
        // File or Blob
        //file = Uri.fromFile(new File("path/to/mountains.jpg"));

        // Create the file metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        final String childName = "imagenes/" + file.getLastPathSegment();
        StorageReference storageRef = getmStorageRef().child(childName);
        // Upload file and metadata to the path 'images/mountains.jpg'
        UploadTask uploadTask = storageRef.putFile(file, metadata);

        // Listen for state changes, errors, and completion of the upload.
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                System.out.println("Upload is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Handle successful uploads on complete
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                saveUriAndName(downloadUrl, childName);

            }
        });
    }

    private void saveUriAndName(Uri downloadUrl, String childName) {

        ImageUpload imageUpload = new ImageUpload(childName,downloadUrl.toString());
        childName = childName.substring(0,childName.lastIndexOf("."));
        DatabaseReference myRef = database.getReference(childName);
        myRef.setValue(imageUpload);
    }

    private String imageName() {
        Long timestamp = System.currentTimeMillis() / 1000;
        return timestamp.toString() + ".jpg";

    }

    private InputStream bitmapToInputStream (Bitmap unBitmap){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        unBitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        return new ByteArrayInputStream(bitmapdata);

    }

    private Bitmap biteArrayToBitmap (byte[] image){
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length, options);
        return bitmap;
    }


    void downloadUserPicture(final ImageView userPicture) throws IOException {

        StorageReference userPictureRef = getmStorageRef().child("admin/userPicture.jpg");
        final long ONE_MEGABYTE = 1024 * 1024;
        userPictureRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] image) {
                // Data for "images/island.jpg" is returns, use this as needed
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length, options);
                userPicture.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
}
