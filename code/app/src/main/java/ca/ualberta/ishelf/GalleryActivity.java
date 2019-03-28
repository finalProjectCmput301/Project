package ca.ualberta.ishelf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.UUID;

/**
 * GalleryActivity
 *
 * Allows a user to see the associated images of a book
 * Allows for you to see an expanded view of the image if yyou click on the image
 * Allows for oyui to add a new image
 *
 * @author : Faisal
 */

public class GalleryActivity extends AppCompatActivity {


    private int colomn_number = 2;
    private final int PICK_IMAGE_FOR_GALLERYY = 36;
    private final int DELETE_IMAGE = 37;

    private ArrayList<String> imageList = new ArrayList<String>();

    private String ID;
    private Book book;

    private RecyclerView galleryRecyclerView;
    private GalleryAdapter galleryAdapter;
    private RecyclerView.LayoutManager galleryLayoutManager;

    private Button addButton;

    // FireBase stuff
    FirebaseStorage storage;
    StorageReference storageReference;

    private Uri lastImagePath;

    /**
     * OnCreate
     * <p>
     * Initializes recyclerView and button
     *
     * @author : Faisal
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // set up FireBase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // get ID for book
        Bundle extras = getIntent().getExtras();
        book = extras.getParcelable("sent_book");

        imageList = book.getGalleryImages();

        // create recycler for images
        galleryRecyclerView = (RecyclerView) findViewById((R.id.gallery_recycler));
        galleryLayoutManager = new GridLayoutManager(this, colomn_number);
        galleryRecyclerView.setLayoutManager(galleryLayoutManager);
        galleryAdapter = new GalleryAdapter(this, imageList);
        galleryRecyclerView.setAdapter(galleryAdapter);

        // set up add button
        addButton = (Button) findViewById((R.id.add_image_button));
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });
    }

    /**
     * pickImage
     * <p>
     * pick Image from Google folder
     * <p>
     * Based on: Android: How to select an Image from a file manager after clicking on a Button?
     * https://stackoverflow.com/questions/35028251/android-how-to-select-an-image-from-a-file-manager-after-clicking-on-a-button
     * User: Ahsan Kamal
     * <p>
     * And based on documentation:
     * https://developer.android.com/training/camera/photobasics
     *
     * @author : Faisal (based on Ahsan Kamal)
     */

    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_FOR_GALLERYY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_FOR_GALLERYY && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            try {
                lastImagePath = data.getData();
                InputStream inputStream = this.getContentResolver().openInputStream(data.getData());
                Bitmap addedImage = BitmapFactory.decodeStream(inputStream);

                String pathImage = "images/" + UUID.randomUUID().toString();

                // add path to Book in FireBase
                Database db = new Database(this);
                db.connect(this);
                book.addImage(pathImage);
                db.editBook(book);

                // store in Storage
                StorageReference ref = storageReference.child(pathImage);
                ref.putFile(lastImagePath)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(GalleryActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(GalleryActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            }
                        });


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == DELETE_IMAGE && resultCode == Activity.RESULT_OK) {
            // implement this
        }
    }


    public void updateImages(){
        galleryAdapter.updateList(imageList);
        galleryAdapter.notifyDataSetChanged();
    }



}

