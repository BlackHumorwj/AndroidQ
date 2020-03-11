package com.example.androidq;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {


    /*
     <!--代表外部存储区域的根目录下的文件 Environment.getExternalStorageDirectory()/DCIM/camerademo目录-->
    <external-path name="hm_DCIM" path="DCIM/camerademo" />

    <!--代表外部存储区域的根目录下的文件 Environment.getExternalStorageDirectory()/Pictures/camerademo目录-->
    <external-path name="hm_Pictures" path="Pictures/camerademo" />

    <!--代表app 私有的存储区域 Context.getFilesDir()目录下的images目录 /data/user/0/com.hm.camerademo/files/images-->
    <files-path name="hm_private_files" path="images" />

    <!--代表app 私有的存储区域 Context.getCacheDir()目录下的images目录 /data/user/0/com.hm.camerademo/cache/images-->
    <cache-path name="hm_private_cache" path="images" />

    <!--代表app 外部存储区域根目录下的文件 Context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)目录下的Pictures目录-->
    <!--/storage/emulated/0/Android/data/com.hm.camerademo/files/Pictures-->
    <external-files-path name="hm_external_files" path="Pictures" />

    <!--代表app 外部存储区域根目录下的文件 Context.getExternalCacheDir目录下的images目录-->
    <!--/storage/emulated/0/Android/data/com.hm.camerademo/cache/images-->
    <external-cache-path name="hm_external_cache" path="" />

     */

    /*
     思路：
       1、AndroidQ中沙箱存储对应的目录地址，怎么通过API获取这些目录地址；
       2、获取相册图片兼容Q 的方式；
       3、将相册中的图片拷贝到 应用沙箱中进行压缩处理；
       4、将沙箱图片拷贝到外部存储的公共目录下 。
       5、xml 中 external-files-path --> 对应沙箱存储中  getExternalFilesDir(Environment.DIRECTORY_PICTURES) 的Pictures



       参考：
        https://blog.csdn.net/azhoup/article/details/102834531
        https://open.oppomobile.com/wiki/doc#id=10432
     */


    private ArrayList<ImageItem> mlist = new ArrayList<>();
    private ImageView mImageView;

    private Context mContext;
    private File mNewFile;

    String[] perms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final int RC_CAMERA_AND_WRITE = 909;
    String path = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mContext = this;
        mImageView = findViewById(R.id.image);


        findViewById(R.id.btn_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeAlbum();
            }
        });

        findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeCamera();
            }
        });


    }


    @AfterPermissionGranted(RC_CAMERA_AND_WRITE)
    private void takeCamera() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            takePhoto(mContext);
        } else {
            EasyPermissions.requestPermissions(this, "申请权限", RC_CAMERA_AND_WRITE, perms);
        }
    }

    @AfterPermissionGranted(RC_CAMERA_AND_WRITE)
    private void takeAlbum() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            pickAlbum();
        } else {
            EasyPermissions.requestPermissions(this, "申请权限", RC_CAMERA_AND_WRITE, perms);
        }
    }


    /**
     * 选择相册
     */
    private void pickAlbum() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取相册集合
                final List<ImageFolder> imageFolders = WImageLoader.newInstance().getImageFolders(mContext);
                mlist.clear();
                for (ImageFolder item : imageFolders) {
                    mlist.addAll(item.images);
                }
                final ImageItem imageItem = mlist.get(0);

                //相册中图片的URI
                String imageUri = imageItem.attachmentUrl;

                //将图片复制一份到自身的App-specific 目录下文件,
                // 然后使用 Luban 设置成App-specific目录下的文件夹
                final File file = FileUtil.compressPic(mContext, Uri.parse(imageUri), FileUtil.getImageName("IMG_ARTWORK"));

                if (file != null) {
                    path = file.getAbsolutePath();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(BitmapFactory.decodeFile(path));
                    }
                });

            }
        }).start();
    }


    private void takePhoto(Context context) {
        //图片直接保存到沙箱内
        final File picDir = FileUtil.getAppSpecificPicDir(mContext);

        //相机拍照
        String picName = FileUtil.getImageName("IMG_CAMERA_ARTWORK");

        String filePath = picDir.getAbsolutePath() + "/" + picName;


        mNewFile = new File(filePath);

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", mNewFile);
        } else {
            uri = Uri.fromFile(mNewFile);
        }

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

        startActivityForResult(intent, 22);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 22:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final File file = FileUtil.compressPic(mContext, mNewFile);
                            if (file != null) {
                                final Uri compressed = FileUtil.insertImage(mContext, FileUtil.getImageName("IMG_COMPRESSED"));

                                final boolean uri = FileUtil.file2Uri(mContext, compressed, file);

                                Log.e("xxx", uri + "");


                                path = file.getAbsolutePath();
                            } else {

                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mImageView.setImageBitmap(BitmapFactory.decodeFile(path));
                                }
                            });
                        }
                    }).start();

                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRationaleAccepted(int requestCode) {

    }

    @Override
    public void onRationaleDenied(int requestCode) {

    }
}
