/*
*   sakiBomb            15Sep24    Created RenamePartsMain
*   sakiBomb-01         15Oct04    rename pics without timestamp
*
*
*
**/






package saki_bomb.ciscorenamepartsapp;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class RenamePartsMain extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    //Main.xml parts
    ImageView mPicSample;
    EditText mRenameText;

    //static File mImagePath;
    static String mCurrentImagePath;  //directory in string
    private Uri mCurrentImageUri;    //actual pic location
    static File mImagePath;   //directory as a file
    static String mTmpFileName; //only the name of the file (stores just timestamp for uniqueness)
    static File mTempImage;   //actual image that was saved

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_rename_parts_main);
        setContentView(R.layout.main);

        //set main.xml parts
        mPicSample = (ImageView) findViewById(R.id.imgView);
        mRenameText = (EditText) findViewById(R.id.file_name_edit);

        //if(savedInstanceState.getString("thumbnailUri") != null)
        //TODO: figure out where to call this
        //    onRestoreInstanceState(savedInstanceState);

        //clear rename edit text box button
        final Button clearRename = (Button) findViewById(R.id.clear_name_text);
        clearRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenameText.setText("");
            }
        });

        final Button submit = (Button) findViewById(R.id.submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

              //save off picture with new name
                String new_file_name_string;
                new_file_name_string = mRenameText.getText().toString() + ".jpg"; //sakiBomb-01

                //file to store final picture in
                File finalFile = new File(mImagePath, new_file_name_string);

                //rename current file to final file

                if(mTempImage.renameTo(finalFile))
                {
                    //remove the old file
                    mTempImage.delete();

                    //update both deletion of old file and creation of new file
                    galleryUpdate(Uri.fromFile(finalFile));
                    galleryUpdate(Uri.fromFile(mTempImage));

                    //remove thumbnail and name
                    mPicSample.setImageURI(null);
                    mRenameText.setText("");

                    //notify user that the pic was saved
                    Toast.makeText(getApplicationContext(), "saved file: " + new_file_name_string,
                            Toast.LENGTH_LONG).show();

                }
                else
                {
                   //TODO:if renaming fails
                }




            }
        });



        final Button takePicButton = (Button) findViewById(R.id.takePicButton);
        takePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //if(mCurrentImageUri==null) TODO: need to take care of when mCurrImageUri is not null bc coming back from interrupted cycle
                    mCurrentImageUri = getImageFileUri();

                //set up camera intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp")));
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentImageUri); // set the image file name
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

            }
        });

        final Button scanBarcode = (Button) findViewById(R.id.scanBarcodeButton);
        scanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(RenamePartsMain.this);
                integrator.initiateScan();
            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    //Set thumbnail of picture
                    mPicSample.setImageURI(mCurrentImageUri);
                    break;
                case IntentIntegrator.REQUEST_CODE:
                    /*
                    * Scanning barcode should just fill up name section
                    * With each scan the name section will become more populated
                    *
                    * Acutally assigning the name can occur when hitting submit button
                    * */

                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (scanResult != null) {
                        //edit rename box with scanned info
                        mRenameText.append(scanResult.getContents().replaceAll("\\s", "_"));

                    }
                    else
                    {
                        // else continue with any other code you need in the method
                        //TODO: handle invalid scan
                    }
                    break;

                default:
                    break;
            }



        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rename_parts_main, menu);
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

    private boolean renameFile(File oldfile, File newFile)
    {
        //create file that we want to store picture at
        File finalFile = new File(mImagePath, "newName" + mTmpFileName);

        //rename current file to final file
        mTempImage.renameTo(finalFile);
        //remove the old file
        mTempImage.delete();

        //update both deletion of old file and creation of new file
        galleryUpdate(Uri.fromFile(finalFile));
        galleryUpdate(Uri.fromFile(mTempImage));

        return false;

    }

    private String parseBarcodeScan()
    {

        return null;
    }

    private void galleryUpdate(Uri imageUri) {
        /**
         * Update file at imageUri
         */
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }


    private static Uri getImageFileUri(){

        // Create a storage directory for the images
        // TODO: To be safe(er), you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this
        mImagePath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Cicon_Pics");
        //Log.d(tag,"Find "+mImagePath.getAbsolutePath());
        if (! mImagePath.exists()){
            if (! mImagePath.mkdirs()){
                Log.d("CameraTestIntent", "failed to create directory");
                return null;
            }else{
                //Log.d(tag,"create new Tux folder");
            }
        }

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mTmpFileName = "" + timeStamp + ".jpg";
        File image = new File(mImagePath, mTmpFileName);
        mTempImage = image;


        if(!image.exists()){
            try {
                image.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


        //return image;

        // Create an File Uri
        return Uri.fromFile(image);
    }

    /*
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("mRenameText", mRenameText.getText().toString());
        if(mCurrentImageUri != null)
            savedInstanceState.putString("thumbnailUri", mCurrentImageUri.toString());
        else
            savedInstanceState.putString("thumbnailUri", null);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String renameText = savedInstanceState.getString("mRenameText");
        String thumbnailUriStr = savedInstanceState.getString("thumbnailUri");

        if(thumbnailUriStr != null)
        {
            File imageFile = new File(thumbnailUriStr);
            mPicSample.setImageURI(Uri.fromFile(imageFile));
        }

        mRenameText.setText(renameText);

    }
*/
}
