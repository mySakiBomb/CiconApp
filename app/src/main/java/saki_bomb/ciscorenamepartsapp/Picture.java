package saki_bomb.ciscorenamepartsapp;

/**
 * Created by saki on 10/6/15.
 */
public class Picture {
    String mPathDir; //String of Directory
    String mName;    //Name of the file without file type(i.e. jpg, png)
    int mDuplicateId = 0;  //internal counter used for Pictures with duplicate file names. 0 means unused

    public String getFullPath()
    {
        return mPathDir + mName + ".jpg";
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
            newName = mName + "(" + mDuplicateId + ")";

        }
        else
        {
            //first time appending counter to name
            mDuplicateId++;
            newName = mName + "(" + mDuplicateId + ")";
        }

        setName(newName);
    }



}
