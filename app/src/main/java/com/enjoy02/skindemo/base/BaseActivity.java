package com.enjoy02.skindemo.base;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;

import com.enjoy02.skindemo.skin.SkinEngine;
import com.enjoy02.skindemo.skin.SkinFactory;

import java.io.File;

/**
 * 把换肤的功能定义在这里
 */
public class BaseActivity extends AppCompatActivity {

    protected static String[] skins = new String[]{"skin.apk", "skin2.apk"};

    protected static String mCurrentSkin = null;

    private SkinFactory mSkinFactory;

    private boolean ifAllowChangeSkin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: 关键点1：hook（劫持）系统创建view的过程
        // hook，拦截住系统创建View的过程，因为系统创建View，也是用反射来执行的，
        // 所以，这里我要先看看系统是如何将layout.xml里面的xml标签，转化成java 的View对象
        if (ifAllowChangeSkin) {
            mSkinFactory = new SkinFactory();//难道和Factory类有关系咯。
            mSkinFactory.setDelegate(getDelegate());//委托类，系统委托类，为了执行系统的创建过程
            // 可见，我们是先执行了系统的委托类逻辑，然后创建我们自己的支持换肤的View
            LayoutInflater layoutInflater = LayoutInflater.from(this);//这个LayoutInflater 在每个Activity中都是独立的，并不是单例
            Log.d("layoutInflaterTag", layoutInflater.toString());//这是BaseActivity，每个继承它的Activity都会打印这个日志，从日志中可以发现，对象地址是不同的
            layoutInflater.setFactory2(mSkinFactory);//设置工厂到LayoutInflater 布局实例化类 setFactory进去,为什么 设置了Factory2之后，就能hook系统创建view的过程?
            //那就要看系统创建view的过程中（其实也就是layoutInflater的源码）Factory扮演了什么样的角色了.
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("changeTag", null == mCurrentSkin ? "currentSkin是空" : mCurrentSkin);

        if (null != mCurrentSkin)
            changeSkin(mCurrentSkin); // 换肤操作必须在setContentView之后
    }

    protected String getPath() {
        String path;
        if (null == mCurrentSkin) {
            path = skins[0];
        } else if (skins[0].equals(mCurrentSkin)) {
            path = skins[1];
        } else if (skins[1].equals(mCurrentSkin)) {
            path = skins[0];
        } else {
            return "unknown skin";
        }
        return path;
    }

    protected void changeSkin(String path) {
        if (ifAllowChangeSkin) {
            //现在我是从手机存储的根目录下放一个skin.apk,真正使用的时候肯定要创建一个皮肤包目录比如：项目名/skin/skin.apk
            File skinFile = new File(Environment.getExternalStorageDirectory(), path);
            //参数1，parent目录，也就是手机内存的根目录,参数2 ，是文件相对于parent的相对路径
            SkinEngine.getInstance().load(skinFile.getAbsolutePath());
            mSkinFactory.changeSkin();
            mCurrentSkin = path;
        }
    }

    //关键是，换了之后。我想起来了，是这个Resource是单例的，艹。一旦读取插件中的资源文件，app内所有资源文件就会被整体换掉。
    // 并不是LayoutInflater单例。

}
