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
     * 复制文件到私有目录下
     *
     * @param fileUri 要复制文件的uri
     * @param newFile 新文件的对象
     * @return 是否复制成功
     */
    public static boolean copyFileToAppSpecificDir(Context context, String fileUri, File newFile) {
        FileInputStream fileOS = null;
        //将图片Uri文件拷贝到 沙箱的目录中
        try {
            fileOS = getFileInputStream(context, fileUri);
            return writeFileFromIS(newFile, fileOS, false);
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
            return writeFileFromIS(newFile, fileOS, false);
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
    public static FileInputStream getFileInputStream(Context context, String fileUri) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse(fileUri), "r");
            if (parcelFileDescriptor != null) {
                return new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            } else {
                return null;
            }
        } else {
            return new FileInputStream(uri2File(context, Uri.parse(fileUri)));
        }
    }

    public static FileInputStream getFileInputStream(Context context, File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }


    public static File uri2File(Context context, Uri uri) {
        String imgPath = "";
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) {
            imgPath = uri.getPath();
        } else {
            int actualImageColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            imgPath = cursor.getString(actualImageColumnIndex);
        }
        File file = new File(imgPath);
        if (cursor != null) {
            cursor.close();
        }
        return file;

    }


    public static boolean writeFileFromIS(File file, InputStream is, boolean append) {
        if (file == null || is == null)
            return false;
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file, append));
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
    public static File compressPic(Context context, String uriPath, String newFileName) {
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

    //将文件保存到公共的媒体文件夹
    //这里的filepath不是绝对路径，而是某个媒体文件夹下的子路径，和沙盒子文件夹类似
    //这里的filename单纯的指文件名，不包含路径
    public void saveSignImage(Context context, String filePath, String fileName, Bitmap bitmap) {

        OutputStream outputStream = null;
        try {
            Uri uri = insertImage(context, filePath, fileName);
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
     *  文件存入公共文件夹中，返回Uri,此时并没有真正插入
     * @param context  Context
     * @param filePath 文件的原路径
     * @param fileName 插入后的文件名
     * @return
     */

    public static Uri insertImage(Context context, String filePath, String fileName) {

        try {
            //设置保存参数到ContentValues中
            ContentValues contentValues = new ContentValues();
            //设置文件名
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            //兼容Android Q和以下版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
                //RELATIVE_PATH是相对路径不是绝对路径
                //DCIM是系统文件夹，关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Pictures");
                //contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Music/signImage");
            } else {
                //contentValues.put(MediaStore.Images.Media.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
                contentValues.put(MediaStore.Images.Media.DATA, filePath);
            }
            //设置文件类型
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");
            //执行insert操作，向系统文件夹中添加文件
            //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            return uri;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将file文件拷贝到 uri路径中
     * @param context Context
     * @param uri 接收数据端
     * @param file 源文件
     * @return
     */
    public static boolean writeFile(Context context, Uri uri, File file) {
        FileOutputStream outputStream = null;
        FileInputStream fileInputStream = null;

        if (uri == null) {
            return false;
        }

        try {
            AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "rw");
            if (assetFileDescriptor != null) {
                outputStream = assetFileDescriptor.createOutputStream();
                fileInputStream = new FileInputStream(file);
                int len;
                byte[] data = new byte[KB];
                while ((len = fileInputStream.read(data, 0, KB)) != -1) {
                    outputStream.write(data, 0, len);
                }
                return true;
            } else {
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeIO(outputStream, fileInputStream);
        }

        return false;
    }


}
