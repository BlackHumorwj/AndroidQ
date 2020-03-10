package com.example.androidq;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * 相片的信息
 */
public class ImageItem implements Serializable, Parcelable {
    public String fileName;       //图片的名字
    public String extName;       //后缀名
    public String attachmentUrl;       //图片的路径
    public String attachmentPath;       //图片上传的相对路径
    public long fileSize;         //图片的大小
    public int width;         //图片的宽度
    public int height;        //图片的高度
    public String fileType;   //图片的类型
    public long addTime;      //图片的创建时间
    public boolean isSelected;
    public String attachmentMiNiUrl;

    public ImageItem() {

    }


    @Override
    public String toString() {
        return "{"
                +"attachmentUrl:"+attachmentUrl
                +"extName:"+extName
                +"fileName:"+fileName+
                "}";
    }

    /**
     * 图片的路径和创建时间相同就认为是同一张图片
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ImageItem) {
            ImageItem item = (ImageItem) o;
            return this.attachmentUrl.equalsIgnoreCase(item.attachmentUrl) && this.addTime == item.addTime;
        }

        return super.equals(o);
    }

    protected ImageItem(Parcel in) {
        fileName = in.readString();
        extName = in.readString();
        attachmentUrl = in.readString();
        attachmentPath = in.readString();
        fileSize = in.readLong();
        width = in.readInt();
        height = in.readInt();
        fileType = in.readString();
        addTime = in.readLong();
        isSelected = in.readByte() != 0;
        attachmentMiNiUrl = in.readString();
    }

    public static final Creator<ImageItem> CREATOR = new Creator<ImageItem>() {
        @Override
        public ImageItem createFromParcel(Parcel in) {
            return new ImageItem(in);
        }

        @Override
        public ImageItem[] newArray(int size) {
            return new ImageItem[size];
        }
    };



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileName);
        dest.writeString(extName);
        dest.writeString(attachmentUrl);
        dest.writeString(attachmentPath);
        dest.writeLong(fileSize);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(fileType);
        dest.writeLong(addTime);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeString(attachmentMiNiUrl);
    }

}