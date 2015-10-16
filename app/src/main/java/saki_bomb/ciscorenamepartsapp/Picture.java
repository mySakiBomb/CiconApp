/**
 *      sakiBomb            15Oct06    Created Picture.java
 *      sakiBomb-06         15Oct11    Fix renaming issue
 *
 */



package saki_bomb.ciscorenamepartsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

/**
 * Created by saki on 10/6/15.
 */
public class Picture {
    String mPathDir; //String of Directory
    String mName;    //Name of the file without file type(i.e. jpg, png)
    File mFile;      //File created using mPathDIr and mName
    int mDuplicateId = 0;  //internal counter used for Pictures with duplicate file names. 0 means unused

    public Picture(String dir, String name)
    {
        mPathDir = dir;
        mName = name;

    }

    public Picture()
    {}

    public String getFullPath()
    {
        return mPathDir + "/" + mName + ".jpg";
    }

    public String getName()
    {
        return mName;
    }

    public String getPath()
    {
        return mPathDir;
    }

    public void setPath(String newPath)
    {
        mPathDir = newPath;
    }

    public void setName(String newName)
    {
        mName = newName;
    }

    /*
    * Will increment counter after name.
    * Used for cases where a given filename might be taken already
    * */
    public void incName()
    {
        String newName;
        int removeLength;

        if(mDuplicateId != 0)
        {
            //Need to remove counter attached on mName and replace it with new id
            removeLength = String.valueOf(mDuplicateId).length() + 2;
            mDuplicateId++;

            newName = mName.substring(0, mName.length() - removeLength);
            newName += "(" + mDuplicateId + ")";    /*sakiBomb-06*/

        }
        else
        {
            //first time appending counter to name
            mDuplicateId++;
            newName = mName + "(" + mDuplicateId + ")";
        }

        setName(newName);
    }

    public void createPicFile()
    {
        mFile = new File(mPathDir, mName);

        if(mFile.exists())
        {
            mFile.delete();
        }

        //create a fresh batch
            try {
                mFile.createNewFile();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
    }

    public Uri getPicUri()
    {
        if(mFile != null)
            return Uri.fromFile(mFile);

        return null; //error
    }

    public File getFile()
    {
        return mFile;
    }


    /*
    *   This method assumes that an image exists at the file location
    *   given by mPath + mName.
    *
    * */
    public Bitmap embedFileNameToImage()
    {
        Bitmap originalImage = BitmapFactory.decodeFile(getFullPath());
        //TODO: make more memory efficient:
        // http://stackoverflow.com/questions/4349075/bitmapfactory-decoderesource-returns-a-mutable-bitmap-in-android-2-2-and-an-immu/9194259#9194259
        Bitmap copyOriginalImage = originalImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(copyOriginalImage);

        //Set up box border for text overlay
        Paint boxInfo = new Paint();
        boxInfo.setStyle(Paint.Style.FILL);
        boxInfo.setColor(Color.BLACK);
        RectF blackBox = new RectF(0, canvas.getHeight() - 100, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(blackBox, boxInfo);

        //setup text
        Paint text_info = new Paint();
        text_info.setColor(Color.WHITE);
        text_info.setTextSize(40);
        canvas.drawText(mName + ".jpg", 0, canvas.getHeight() - 40, text_info);

        //originalImage should be loaded with an embedded picture at this point
        return copyOriginalImage;

    }



}
