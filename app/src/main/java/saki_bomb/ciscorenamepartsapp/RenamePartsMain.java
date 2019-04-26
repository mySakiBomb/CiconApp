/*
*   sakiBomb            15Sep24    Created RenamePartsMain
*   sakiBomb-01         15Oct04    rename pics without timestamp
*   sakiBomb-02         15Oct05    Replace ZXing barcode reader with QR droid barcode reader
*                                  TODO: incorporate checking qrdroid is installed using services
*   sakiBomb-03         15Oct05    TODO: Change naming convention to append count (for doubles)
*   sakiBomb-04         15Oct07    Refactor using Picture class
*   sakiBomb-05         15Oct09    Text embedding in Image
*   sakiBomb-06         15Oct11    Fix renaming picture error
*   sakiBomb-07         15Oct11    Check for null pics and null name values on submit
*   sakiBomb-08         15Oct15    New UI. Now using actionbar
*   sakiBomb-09         15Oct15    Created new method to handle old button logic (i.e. pic, qr scan)
*   sakiBomb-10         15Oct17    Handle directory changes
*   sakiBomb-11         15Oct24    use new scanner SCANDIT for qr reading
*   sakiBomb-12         15Oct30    incorporate inigma scanner (trial version)
*   sakiBomb-13         15Nov20    Add setup phase to acquire inigma licensing
*   sakiBomb-14         15Dec01    Remove calcCameraSize() to prevent lens open-close-open
*   sakiBomb-15         16Feb17    Save pics to memory card instead of default external storage
*   sakiBomb-16         16Feb17    remove \r\n from directory names
*   sakiBomb-17         16Mar04    add gallery app intent

*
**/


package saki_bomb.ciscorenamepartsapp;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.widget.ImageButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

//zxing
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.threegvision.products.inigma_sdk_pro.sdk_pro.SDK;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import saki_bomb.ciscorenamepartsapp.InigmaScanner.InigmaScanActivity;


public class RenamePartsMain extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE     = 1;
    static final int REQUEST_SCAN_QRDROID_NAME = 2;
    static final int REQUEST_SCAN_QRDROID_DIR  = 3;
    static final int REQUEST_SCAN_SCANDIT_NAME = 4;
    static final int REQUEST_SCAN_SCANDIT_DIR  = 5;
    static final int REQUEST_SCAN_INIGMA_NAME  = 6;
    static final int REQUEST_SCAN_INIGMA_DIR   = 7;
    static final int REQUEST_GALLERY           = 8; /*sakiBomb-17*/

    static final String DEFAULT_DIR = "/Cicon_Pics";
    static final String DEFAULT_EXT_DIR = "/storage/extSdCard";

    //Main.xml parts
    ImageView mPicSample;
    EditText mRenameText;
    EditText mDirectory;   /*sakiBomb-10*/

    //using Picture class  sakiBomb-04
    static Picture mPicture;
    static Picture mTmpPicture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_rename_parts_main);
        //setContentView(R.layout.main);
        setContentView(R.layout.cicon_rename_main_actionbar);


        /*sakiBomb-08-->*/
        //add logo to action bar
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setLogo(R.drawable.cicon);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
        /*<--sakiBomb-08*/

        /*Setup InigmaScannerActivity Licensing*/
        /*SakiBomb-13 -->*/
        Intent InigmaSetupIntent = new Intent(this, InigmaScanActivity.class);
        InigmaSetupIntent.putExtra("isSetup", true);
        startActivity(InigmaSetupIntent);
        /*<-- SakiBomb-13 */

        CreateDefaultDirectory();

        // Create a storage directory for the images
        // TODO: To be safe(er), you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this
        //TODO: use getExternalStorageDirectory to save to SD card
        /*mTmpPicture = new Picture(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + "/Cicon_Pics", "tmpImage");  //used to save initial picture
        */
        mTmpPicture= new Picture(DEFAULT_EXT_DIR, "tmpExtImage");            //sakiBomb-15
        mPicture = new Picture();


        //set main.xml parts and initialize
        mPicSample = (ImageView) findViewById(R.id.imgView);
        mRenameText = (EditText) findViewById(R.id.file_name_edit);
        mRenameText.setText("");
        //if(savedInstanceState.getString("thumbnailUri") != null)

        mDirectory = (EditText) findViewById(R.id.directory_name_field);

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
                HandleSubmit();
            }
        });

        /* sakiBomb-17 -->*/
        final Button gallery = (Button) findViewById(R.id.gallery_button);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(
                        Intent.ACTION_VIEW,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivity(galleryIntent);
            }
        });
        /* <-- sakiBomb-17 */

    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    public void onBackPressed() {
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String scannedFileName;
        String scannedDirName;

        /*TODO: replace scanned result with method variables instead
        *       of using new strings for each case
        */
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    //Set thumbnail of picture
                    mPicSample.setImageURI(mTmpPicture.getPicUri());
                    break;
                case IntentIntegrator.REQUEST_CODE:
                    //Return from ZXing Code Currently unused
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (scanResult != null) {
                        //edit rename box with scanned info
                        mRenameText.append(scanResult.getContents().replaceAll("\\s", "_"));
                    } else {
                        // else continue with any other code you need in the method
                        //TODO: handle invalid scan
                    }
                    break;
                case REQUEST_SCAN_QRDROID_NAME:
                    String result = data.getExtras().getString("la.droid.qr.result");
                    mRenameText.append(result.replaceAll("\\s", "_"));
                    break;
                /*sakiBomb-10-->*/
                case REQUEST_SCAN_QRDROID_DIR:
                    String result_dir = data.getExtras().getString("la.droid.qr.result");
                    if (result_dir.charAt(0) == '/')
                        mDirectory.setText(DEFAULT_DIR + result_dir);
                    else
                        mDirectory.setText(DEFAULT_DIR + '/' + result_dir);
                    break;
                /*<--sakiBomb-10*/
                /*SakiBomb-11 -->*/
                case REQUEST_SCAN_SCANDIT_NAME:
                    scannedFileName = data.getExtras().getString("SCAN_RETURN");
                    mRenameText.append(scannedFileName.replaceAll("\\s", "_"));
                    break;
                case REQUEST_SCAN_SCANDIT_DIR:
                    scannedDirName = data.getExtras().getString("SCAN_RETURN");
                    if (scannedDirName.charAt(0) == '/')
                        mDirectory.setText(DEFAULT_DIR + scannedDirName);
                    else
                        mDirectory.setText(DEFAULT_DIR + '/' + scannedDirName);
                    break;
                /*<-- SakiBomb-11 */  /*SakiBomb-12-->*/
                case REQUEST_SCAN_INIGMA_NAME:
                    scannedFileName = data.getExtras().getString("SCAN_RETURN");
                    mRenameText.append(scannedFileName);
                    break;
                case REQUEST_SCAN_INIGMA_DIR:
                    scannedDirName = data.getExtras().getString("SCAN_RETURN");
                    if (scannedDirName.charAt(0) == '/')
                        mDirectory.setText(DEFAULT_DIR + scannedDirName);
                    else
                        mDirectory.setText(DEFAULT_DIR + '/' + scannedDirName);
                    break;
                /*<--SakiBomb-12*/

                default:
                    break;
            }
        }
    }

    //MENU Handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rename_parts_main, menu); //sakiBomb-08

        //Handler for directory button (note it is not an icon like pic and scanner)
        /*sakiBomb-10-->*/
        ImageButton dirButton = (ImageButton) menu.findItem(R.id.directoryButton).getActionView();
        Drawable d = getResources().getDrawable(R.drawable.smalldirectory);
        dirButton.setImageDrawable(d);

        //Set Directory button listeners
        dirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartScanQRDirIntent();
            }
        });

        dirButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(),
                        "Default Directory: " + DEFAULT_DIR, Toast.LENGTH_LONG).show();
                ResetDirectory();
                return true;
            }
        });
        /*<--sakiBomb-10*/

        //return true to stop further click processing (i.e. prevent OnClickListener execution)
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*sakiBomb-08-->*/
        if (id == R.id.takePicItem) {
            StartTakePicIntent();
            return true;
        } else if (id == R.id.scanBarcodeItem) {
            StartScanQRNameIntent();
            return true;
        }
        //Directory Button taken care by actual button   /*sakiBomb-10*/
         /*<--sakiBomb-08*/
        return super.onOptionsItemSelected(item);
    }


    private void galleryUpdate(Uri imageUri) {
        /*
         * Update file at imageUri
         */
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }


    private void CreateDefaultDirectory() {
        File newExtDir = new File(DEFAULT_EXT_DIR);           //sakiBomb-15

        /*sakiBomb-15 -->*/
        if (!newExtDir.exists())
            if (!newExtDir.mkdirs()) {
                Log.d("CameraTestIntent", "failed to create directory");
            }
        /*<--sakiBomb-15*/
    }


    /*creates given directory if it does not exist*/
    /*sakiBomb-10-->*/
    private void CreateDirectory(Picture finalPic)
    {
        /*sakiBomb-16 remove \r characters from file names -->*/
        finalPic.setPath(finalPic.getPath().replace('\r', '_'));
        finalPic.setPath(finalPic.getPath().replace('\n', '_'));
        File newDir = new File(finalPic.getPath());
        /*<-- sakiBomb-16*/

        if(!newDir.exists())
            if(!newDir.mkdirs())
            {
                Log.d("CreateNewDirectory", "failed to create new directory");
            }
    }
    /*<--sakiBomb-10*/

    /*
     *   Looks through saved pictures in external memory to
     *   see if name is used. If so appends a counter to make file unique
     */
    private File CreateValidFile(Picture pic) {
        //Names will be as follows xxxx_(c).jpg

        File curFile = new File(pic.getPath(), pic.getName() + ".jpg");

        //check if the file already exists:
        while (curFile.exists()) {
            //file already exists, increment name until it's a new file
            pic.incName();
            curFile = new File(pic.getPath(), pic.getName() + ".jpg");
        }

        //at this point filename should now be unique
        return curFile;
    }

    /*sakiBomb-09 -->*/
    private void StartTakePicIntent() {
        /*sakiBomb-15 -->*/
        //only allow user to take pic if an sd card is inserted.
        //TODO: check for sd card
        /*<--sakiBomb-15*/

        //set up camera intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //save image to temp file for now
            mTmpPicture.createPicFile();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTmpPicture.getPicUri());
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void StartScanQRNameIntent() {
        /*Set up intent using Zxing
                IntentIntegrator integrator = new IntentIntegrator(RenamePartsMain.this);
                integrator.initiateScan();
                */

        //Set up intent using QR Droid  sakiBomb-02
        //Intent QRDroidIntent = new Intent("la.droid.qr.scan");
        //startActivityForResult(QRDroidIntent, REQUEST_SCAN_QRDROID_NAME);

        /*SakiBomb-11 -->*/
        /* scan using new SCANDIT intent*/
        //Intent ScanditIntent = new Intent(RenamePartsMain.this, ScanditActivity.class);
        //startActivityForResult(ScanditIntent, REQUEST_SCAN_SCANDIT_NAME);
            /*<--SakiBomb-11 */

        /*SakiBomb-12-->*/
        Intent InigmaIntent = new Intent(this, InigmaScanActivity.class);
        startActivityForResult(InigmaIntent, REQUEST_SCAN_INIGMA_NAME);
            /*<--SakiBomb-12*/
    }

    /*sakiBomb-10-->*/
    private void StartScanQRDirIntent() {
        //Intent scanQrForDirectory = new Intent("la.droid.qr.scan");
        //startActivityForResult(scanQrForDirectory, REQUEST_SCAN_QRDROID_DIR);

        /*SakiBomb-11 -->*/
        //SCAN directory using SCANDIT
        //Intent ScanditIntent = new Intent(RenamePartsMain.this, ScanditActivity.class);
        //startActivityForResult(ScanditIntent, REQUEST_SCAN_SCANDIT_DIR);
        /*<-- SakiBomb-11 */

        /*SakiBomb-12--> */
        Intent InigmaIntent = new Intent(this, InigmaScanActivity.class);
        startActivityForResult(InigmaIntent, REQUEST_SCAN_INIGMA_DIR);
            /*<-- SakiBomb-12 */


    }
    /*<--sakiBomb-10*/

    private void HandleSubmit() {
        //save off picture with new name
        String newName = mRenameText.getText().toString();
        String newDir = mDirectory.getText().toString();

        //error check for blank names and make sure pic was taken  /*sakiBomb-07-->*/
        if (newName.equals("")) {
            Toast.makeText(getApplicationContext(), "Name is empty.", Toast.LENGTH_LONG).show();
            return;
        } else if (mPicSample.getDrawable() == null) {
            Toast.makeText(getApplicationContext(), "Need to take a picture before submitting", Toast.LENGTH_LONG).show();
            return;
        }
                /*<--sakiBomb-07*/


        mPicture.setName(newName); //sakiBomb-03
        //mPicture.setPath(getExternalStoragePath() + newDir);    /*sakiBomb-10*/
        mPicture.setPath(DEFAULT_EXT_DIR + newDir);               /*sakiBomb-15*/

        //make sure directory for final image location is created
        CreateDirectory(mPicture);

        //file to store final picture in
        File finalFile = CreateValidFile(mPicture);


        //rename current file to final file

        if (mTmpPicture.getFile().renameTo(finalFile)) {
            //remove the old file
            mTmpPicture.getFile().delete();

            //tmp image is now saved off into new renamed file
            //note:finalFile is based of mPicture hence using mPicture below
            //now embed name into picture  /*sakiBomb-04*/
            Bitmap finalIamge = mPicture.embedFileNameToImage();
            SaveBitmapToMemory(finalIamge, mPicture);

            //update both deletion of old file and creation of new file
            galleryUpdate(Uri.fromFile(finalFile));
            galleryUpdate(mTmpPicture.getPicUri());

            Toast.makeText(getApplicationContext(), "saved file: " + mDirectory.getText().toString()
                    + '/' + mPicture.getName() + ".jpg", Toast.LENGTH_LONG).show();

            //reset member values:
            mPicSample.setImageURI(null);
            mRenameText.setText("");
            mPicture = new Picture();     /*sakiBomb-06*/ //reset pictures


        } else {
            //TODO:if renaming fails
        }
    }
    /*<--sakiBomb-09*/

    /*SakiBomb-04 -->*/
    private void SaveBitmapToMemory(Bitmap image, Picture outputFile) {
        try {

            //File outFile = new File(outputFile.getPath(), outputFile.getName());
            File outFile = new File(outputFile.getFullPath());
            BufferedOutputStream os = new BufferedOutputStream(
                    new FileOutputStream(outFile));

            image.compress(Bitmap.CompressFormat.JPEG, 100, os);

        } catch (FileNotFoundException e) {
            Log.e("SaveBitmapToMemory", "FileNotFoundException");
        }
    }

    public String getExternalStoragePath()
    {
        return  Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).toString();

    }

    /*<--SakiBomb-04*/


    public void ResetDirectory()
    {
        mDirectory.setText(DEFAULT_DIR);

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
