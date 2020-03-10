package com.example.androidq;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @time 2018/8/30 15:45
 * @desc
 */

public class WImageLoader {

    public static int sMinImageSize = 500;

    private final String[] IMAGE_PROJECTION = {     //查询图片需要的数据列
            MediaStore.Images.Media.DISPLAY_NAME,   //图片的显示名称  aaa.jpg
            MediaStore.Images.Media.DATA,           //图片的真实路径  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
            // MediaStore.Images.Media.SIZE,           //图片的大小，long型  132492
            //   MediaStore.Images.Media.WIDTH,          //图片的宽度，int型  1920
            //   MediaStore.Images.Media.HEIGHT,         //图片的高度，int型  1080
            MediaStore.Images.Media.MIME_TYPE,      //图片的类型     image/jpeg
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media._ID,};    //图片被添加的时间，long型  1450518608

    private ArrayList<ImageFolder> imageFolders = new ArrayList<>();   //所有的图片文件夹


    public static WImageLoader newInstance() {
        WImageLoader fragment = new WImageLoader();
        return fragment;
    }


    public List<ImageFolder> getImageFolders(Context context) {
        //获取相册的图片的Uri
        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        ContentResolver contentResolver = context.getContentResolver();
        //获取Cursor
        Cursor cursor = contentResolver.query(contentUri, IMAGE_PROJECTION, "", null, "");
        if (cursor == null) {
            return imageFolders;
        }
        ArrayList<ImageItem> allImages = new ArrayList<>();   //所有图片的集合,不分文件夹
        while (cursor.moveToNext()) {
            //查询数据
            String imageName = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));

            String imageMimeType = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
            long imageAddTime = cursor.getLong(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));

            //Android Q 公有目录只能通过Content Uri + id的方式访问，以前的File路径全部无效，如果是Video，记得换成MediaStore.Videos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imagePath = MediaStore.Images.Media//
                        .EXTERNAL_CONTENT_URI///
                        .buildUpon()//
                        .appendPath(String.valueOf(id))//
                        .build()//
                        .toString();
            }

            //封装实体
            ImageItem imageItem = new ImageItem();
            imageItem.fileName = imageName;
            imageItem.attachmentUrl = imagePath;
            imageItem.fileType = imageMimeType;
            imageItem.addTime = imageAddTime;
            allImages.add(imageItem);



            //根据父路径分类存放图片
            File imageFile = new File(imagePath);
            File imageParentFile = imageFile.getParentFile();
            ImageFolder imageFolder = new ImageFolder();
            imageFolder.name = imageParentFile.getName();
            imageFolder.path = imageParentFile.getAbsolutePath();


            if (!imageFolders.contains(imageFolder)) {
                ArrayList<ImageItem> images = new ArrayList<>();
                images.add(imageItem);
                imageFolder.cover = imageItem;
                imageFolder.images = images;
                imageFolders.add(imageFolder);
            } else {
                imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
            }
        }


        //防止没有图片报异常
        if (cursor.getCount() > 0 && allImages.size() > 0) {
            //构造所有图片的集合
            ImageFolder allImagesFolder = new ImageFolder();
            allImagesFolder.name = "完成";
            allImagesFolder.path = "/";
            allImagesFolder.cover = allImages.get(0);
            allImagesFolder.images = allImages;
            imageFolders.add(0, allImagesFolder);  //确保第一条是所有图片
        }

        cursor.close();
        return imageFolders;
    }
}
