package com.example.androidq;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

import top.zibin.luban.Luban;

/**
 * @time 2020/3/10 10:21
 * @desc
 */
public class FileUtil {

    /**
     * KB与Byte的倍数
     */
    public static final int KB = 1024;

    public static final String MINE_TYPE_IMAGE_JPEG = "image/JPEG";
    public static final String MINE_TYPE_APK = "application/vnd.android.package-archive";

    /**
     * 获取私有文件夹下图片目录
     *
     * @param context
     * @return
     */
    public static File getAppSpecificPicDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    /**
     * 在私有目录下创建一个文件
     *
     * @param context  Context
     * @param fileName 文件名
     * @return File文件
     */
    public static File newFileInAs(Context context, String fileName) {
        //获取私有目录图片文件夹
        final File primaryDir = getAppSpecificPicDir(context);
        if (primaryDir == null)
            return null;

        //storage/emulated/0/Android/data/com.example.androidq/files/Pictures
        //在目录下的创建一个新的文件对象
        return new File(primaryDir.getAbsolutePath(), fileName);
    }


    /**
     * 复制文件到沙箱私有目录下
     *
     * @param fileUri 要复制文件的uri
     * @param newFile 新文件的对象
     * @return 是否复制成功
     */
    public static boolean copyFileToAppSpecificDir(Context context, Uri fileUri, File newFile) {
        FileInputStream fileOS = null;
        //将图片Uri文件拷贝到 沙箱的目录中
        try {

            fileOS = getFileInputStream(context, fileUri);
            return is2File(newFile, fileOS, false);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeIO(fileOS);
        }
        return false;
    }

    public static boolean copyFileToAppSpecificDir(Context context, File fileUri, File newFile) {
        FileInputStream fileOS = null;
        //将图片Uri文件拷贝到 沙箱的目录中
        try {
            fileOS = getFileInputStream(context, fileUri);

            return is2File(newFile, fileOS, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeIO(fileOS);
        }
        return false;
    }

    /**
     * 获取文件的输入流
     *
     * @param fileUri 原始文件的uri
     * @return FileInputStream
     * @throws FileNotFoundException
     */
    public static FileInputStream getFileInputStream(Context context, Uri fileUri) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "r");
            if (parcelFileDescriptor != null) {
                return new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            } else {
                return null;
            }
        } else {
            return new FileInputStream(getFileFromUri(context, fileUri));
        }
    }

    public static FileInputStream getFileInputStream(Context context, File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    final static String[] proj = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};

    /**
     * 通过uri查找对应的File文件
     *
     * @param context Context
     * @param uri     uri
     * @return
     */
    public static File getFileFromUri(Context context, Uri uri) {
        String imgPath = "";

        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) {
            imgPath = uri.getPath();
        } else {
            imgPath = cursor.getString(cursor.getColumnIndexOrThrow(proj[0]));
        }
        File file = new File(imgPath);
        if (cursor != null) {
            cursor.close();
        }
        return file;

    }

    /**
     * 输入流宝 拷贝文件中
     *
     * @param file   文件
     * @param is     输入流
     * @param append
     * @return
     */
    public static boolean is2File(File file, InputStream is, boolean append) {
        if (file == null || is == null)
            return false;
        try {
            return is2Os(is, new FileOutputStream(file, append));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 输入流 拷贝 到输出流中
     *
     * @param is InputStream
     * @param os OutputStream
     * @return 是否成功
     */
    public static boolean is2Os(InputStream is, OutputStream os) {
        if (is == null || os == null) {
            return false;
        }

        try {
            os = new BufferedOutputStream(os);
            byte data[] = new byte[KB];
            int len;
            while ((len = is.read(data, 0, KB)) != -1) {
                os.write(data, 0, len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeIO(is, os);
        }

    }


    /**
     * 关闭IO
     *
     * @param closeables closeable
     */
    public static void closeIO(Closeable... closeables) {
        if (closeables == null)
            return;
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 压缩图片
     *
     * @param context     Context
     * @param uriPath     原文件路径uri
     * @param newFileName 新文件的文件名
     * @return File
     */
    public static File compressPic(Context context, Uri uriPath, String newFileName) {
        File newFile = FileUtil.newFileInAs(context, newFileName);
        if (newFile == null) {
            return null;
        }

        try {
            //将系统的图片复制一份到 应用沙箱中 并且将压缩后的路径设置为应用沙箱的路径
            final boolean copySuccess = FileUtil.copyFileToAppSpecificDir(context, uriPath, newFile);
            if (copySuccess) {
                //压缩自己私有目录下的文件，并设置压缩后的路径也在私有目录下
                final List<File> files = Luban.with(context)//
                        .load(newFile)//
                        .setTargetDir(FileUtil.getAppSpecificPicDir(context).getAbsolutePath())//设置压缩后的文件目录
                        .get();//

                return files.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

        return null;

    }

    /**
     * @param context
     * @param newFile 原文件
     * @return
     */
    public static File compressPic(Context context, File newFile) {
        //压缩自己私有目录下的文件，并设置压缩后的路径也在私有目录下
        final List<File> files;//
        try {
            files = Luban.with(context)//
                    .load(newFile)//
                    .setTargetDir(FileUtil.getAppSpecificPicDir(context).getAbsolutePath())//设置压缩后的文件目录
                    .get();

            return files.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;

    }

    public static String getImageName(String preName) {
        return preName + "_" + getDateString() + ".jpg";
    }


    //将文件保存到公共的媒体文件夹
    //这里的filepath不是绝对路径，而是某个媒体文件夹下的子路径，和沙盒子文件夹类似
    //这里的filename单纯的指文件名，不包含路径
    public void saveSignImage(Context context, String filePath, String fileName, Bitmap bitmap) {

        OutputStream outputStream = null;
        try {
            Uri uri = insertImage(context, fileName);
            if (uri != null) {
                //若生成了uri，则表示该文件添加成功
                //使用流将内容写入该uri中即可
                outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    outputStream.flush();
                    outputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeIO(outputStream);
        }
    }


    /**
     * 文件存入公共文件夹中，返回Uri,此时并没有真正插入
     *
     * @param context  Context
     * @param fileName 插入后的文件名
     * @return
     */

    public static Uri insertImage(Context context, String fileName) {
        return insert(context, fileName, Environment.DIRECTORY_PICTURES, MINE_TYPE_IMAGE_JPEG);
    }


    /**
     * @param context      Context
     * @param displayName  文件名称
     * @param relativePath 相对路径不是绝对路径，系统自带
     * @param mineType     文件类型
     * @return Uri
     */
    public static Uri insert(Context context, String displayName, String relativePath, String mineType) {

        //设置保存参数到ContentValues中
        ContentValues contentValues = new ContentValues();
        //设置文件名
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        //兼容Android Q和以下版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
            //RELATIVE_PATH是相对路径不是绝对路径
            //DCIM是系统文件夹，关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
            //contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Music/signImage");
        } else {
            //contentValues.put(MediaStore.Images.Media.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
            contentValues.put(MediaStore.Images.Media.DATA, Environment.getExternalStoragePublicDirectory(relativePath).getPath() + "/" + displayName);
        }
        //设置文件类型
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mineType);
        //执行insert操作，向系统文件夹中添加文件
        //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;
    }


    /**
     * 将file文件拷贝到 uri路径中
     *
     * @param context Context
     * @param uri     接收数据端
     * @param file    源文件
     * @return
     */
    public static boolean file2Uri(Context context, Uri uri, File file) {

        if (uri == null || file == null) {
            return false;
        }

        try {
            return is2Uri(context, uri, new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * InputStream 拷贝到 uri 中
     *
     * @param context Context
     * @param uri     Uri
     * @param is      InputStream
     * @return
     */

    public static boolean is2Uri(Context context, Uri uri, InputStream is) {
        FileOutputStream outputStream = null;

        if (uri == null) {
            return false;
        }
        try {
            AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "rw");
            if (assetFileDescriptor != null) {
                outputStream = assetFileDescriptor.createOutputStream();
                return is2Os(is, outputStream);
            } else {
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(outputStream, is);
        }

        return false;
    }


    private static String getDateString() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return "" + year + month + day + "_" + hour + minute + second;
    }


}
