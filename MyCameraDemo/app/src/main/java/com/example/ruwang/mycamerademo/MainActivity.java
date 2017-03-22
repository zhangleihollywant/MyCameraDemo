package com.example.ruwang.mycamerademo;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 我的启动相机拍照demo
 * 调用本地图片
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button photo, choose;
    private Uri mUri;
    private ImageView img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photo = (Button) findViewById(R.id.take_photo);
        img = (ImageView) findViewById(R.id.picture);
        choose = (Button) findViewById(R.id.choose);

        /**
         * 相机拍照
         */
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * ，拍照图片存放在和手机sd卡的应用关联缓存目录下，sd卡专门存放当前应用缓存数据的位置，getExternalCacheDir可以获取该目录！
                 */
                File file = new File(getExternalCacheDir(), "output_img.jpg");
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 分版本实例化路径，如果是安卓7.0以上的手机直接获取uri的话是不安全的，使用了和内部提供器雷士的机制对数据进行保护！
                 */
                if (Build.VERSION.SDK_INT >= 24) {
                    //内容提供器，需要在清单文件中注册
                    mUri = FileProvider.getUriForFile(MainActivity.this, "com.example.ruwang.mycamerademo.fileprovider", file);
                } else {
                    mUri = Uri.fromFile(file);
                }
                /**
                 * 启动相机程序，其实启动相机不需要动态获取权限的！！！！
                 */
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);//指定图片输出地址
                startActivityForResult(intent, 1);
            }
        });
        choose.setOnClickListener(this);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * 注意写法，请求code以及结果code
         */
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
                        img.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            //打开相册选取图片的逻辑
            case 100:
                if (resultCode == RESULT_OK) {
                    //判断手机型号
                    if (Build.VERSION.SDK_INT >= 19) {//4.4以上版本
                        handlerOnKitcat(data);
                    } else {//4.4以下版本
                        handlerBeforeKitcat(data);
                    }
                }
        }
    }

    private void handlerBeforeKitcat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    /**
     * 4.4以上版本uir已经被封装了，需要进行解析
     *
     * @param data
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handlerOnKitcat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的id则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.provider.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.provider.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bit = BitmapFactory.decodeFile(imagePath);
            img.setImageBitmap(bit);
        } else {
            Toast.makeText(this, "提取图片错误", Toast.LENGTH_SHORT).show();
        }
    }

    private String getImagePath(Uri externalContentUri, String selection) {
        String path = null;
        //通过uri和selection获取真实的图片路径
        Cursor cursor = getContentResolver().query(externalContentUri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    @Override
    public void onClick(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openPic();
        }
    }

    private void openPic() {
        //都可以的方法
//        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openPic();
                } else {
                    Toast.makeText(this, "you  denied the perssion", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
