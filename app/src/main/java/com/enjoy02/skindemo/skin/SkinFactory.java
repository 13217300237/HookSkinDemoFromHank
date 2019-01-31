package com.enjoy02.skindemo.skin;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.enjoy02.skindemo.R;
import com.enjoy02.skindemo.view.ZeroView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SkinFactory implements LayoutInflater.Factory2 {

    private AppCompatDelegate mDelegate;//预定义一个委托类，它负责按照系统的原有逻辑来创建view

    private List<SkinView> listCacheSkinView = new ArrayList<>();//我自定义的list，缓存所有可以换肤的View对象

    /**
     * 给外部提供一个set方法
     *
     * @param mDelegate
     */
    public void setDelegate(AppCompatDelegate mDelegate) {
        this.mDelegate = mDelegate;
    }


    /**
     * Factory2 是继承Factory的，所以，我们这次是主要重写Factory的onCreateView逻辑，就不必理会Factory的重写方法了
     *
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    /**
     * @param parent
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {

        // TODO: 关键点1：执行系统代码里的创建View的过程，我们只是想加入自己的思想，并不是要全盘接管
        View view = mDelegate.createView(parent, name, context, attrs);//系统创建出来的时候有可能为空，你问为啥？请全文搜索 “标记标记，因为” 你会找到你要的答案
        if (view == null) {//万一系统创建出来是空，那么我们来补救
            mConstructorArgs[0] = context;
            try {
                if (-1 == name.indexOf('.')) {//不包含. 说明不带包名，那么我们帮他加上包名
                    view = createViewByPrefix(context, name, prefixs, attrs);
                } else {//包含. 说明 是权限定名的view name，
                    view = createViewByPrefix(context, name, null, attrs);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //TODO: 关键点2 收集需要换肤的View
        collectSkinView(context, attrs, view);

        return view;
    }

    /**
     * TODO: 收集需要换肤的控件
     * 收集的方式是：通过自定义属性isSupport，从创建出来的很多View中，找到支持换肤的那些，保存到map中
     */
    private void collectSkinView(Context context, AttributeSet attrs, View view) {
        // 获取我们自己定义的属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Skinable);
        boolean isSupport = a.getBoolean(R.styleable.Skinable_isSupport, false);
        if (isSupport) {//找到支持换肤的view
            final int Len = attrs.getAttributeCount();
            HashMap<String, String> attrMap = new HashMap<>();
            for (int i = 0; i < Len; i++) {//遍历所有属性
                String attrName = attrs.getAttributeName(i);
                String attrValue = attrs.getAttributeValue(i);
                attrMap.put(attrName, attrValue);//全部存起来
            }

            SkinView skinView = new SkinView();
            skinView.view = view;
            skinView.attrsMap = attrMap;
            listCacheSkinView.add(skinView);//将可换肤的view，放到listCacheSkinView中
        }

    }

    /**
     * 公开给外界的换肤入口
     */
    public void changeSkin() {
        for (SkinView skinView : listCacheSkinView) {
            skinView.changeSkin();
        }
    }

    static class SkinView {
        View view;
        HashMap<String, String> attrsMap;

        /**
         * 真正的换肤操作
         */
        public void changeSkin() {
            if (!TextUtils.isEmpty(attrsMap.get("background"))) {//属性名,例如，这个background，text，textColor....
                int bgId = Integer.parseInt(attrsMap.get("background").substring(1));//属性值，R.id.XXX ，int类型，
                // 这个值，在app的一次运行中，不会发生变化
                String attrType = view.getResources().getResourceTypeName(bgId); // 属性类别：比如 drawable ,color
                if (TextUtils.equals(attrType, "drawable")) {//区分drawable和color
                    view.setBackgroundDrawable(SkinEngine.getInstance().getDrawable(bgId));//加载外部资源管理器，拿到外部资源的drawable
                } else if (TextUtils.equals(attrType, "color")) {
                    view.setBackgroundColor(SkinEngine.getInstance().getColor(bgId));
                }
            }

            if (view instanceof TextView) {
                if (!TextUtils.isEmpty(attrsMap.get("textColor"))) {
                    int textColorId = Integer.parseInt(attrsMap.get("textColor").substring(1));
                    ((TextView) view).setTextColor(SkinEngine.getInstance().getColor(textColorId));
                }
            }

            //那么如果是自定义组件呢
            if (view instanceof ZeroView) {
                //那么这样一个对象，要换肤，就要写针对性的方法了，每一个控件需要用什么样的方式去换，尤其是那种，自定义的属性，怎么去set，
                // 这就对开发人员要求比较高了，而且这个换肤接口还要暴露给 自定义View的开发人员,他们去定义
                // ....
            }
        }

    }

    /**
     * 所谓hook，要懂源码，懂了之后再劫持系统逻辑，加入自己的逻辑。
     * 那么，既然懂了，系统的有些代码，直接拿过来用，也无可厚非。
     */
    //*******************************下面一大片，都是从源码里面抄过来的，并不是我自主设计******************************
    // 你问我抄的哪里的？到 AppCompatViewInflater类源码里面去搜索：view = createViewFromTag(context, name, attrs);
    static final Class<?>[] mConstructorSignature = new Class[]{Context.class, AttributeSet.class};//
    final Object[] mConstructorArgs = new Object[2];//View的构造函数的2个"实"参对象
    private static final HashMap<String, Constructor<? extends View>> sConstructorMap = new HashMap<String, Constructor<? extends View>>();//用映射，将View的反射构造函数都存起来
    static final String[] prefixs = new String[]{//安卓里面控件的包名，就这么3种,这个变量是为了下面代码里，反射创建类的class而预备的
            "android.widget.",
            "android.view.",
            "android.webkit."
    };

    /**
     * 反射创建View
     *
     * @param context
     * @param name
     * @param prefixs
     * @param attrs
     * @return
     */
    private final View createViewByPrefix(Context context, String name, String[] prefixs, AttributeSet attrs) {

        Constructor<? extends View> constructor = sConstructorMap.get(name);
        Class<? extends View> clazz = null;

        if (constructor == null) {
            try {
                if (prefixs != null && prefixs.length > 0) {
                    for (String prefix : prefixs) {
                        clazz = context.getClassLoader().loadClass(
                                prefix != null ? (prefix + name) : name).asSubclass(View.class);//控件
                        if (clazz != null) break;
                    }
                } else {
                    if (clazz == null) {
                        clazz = context.getClassLoader().loadClass(name).asSubclass(View.class);
                    }
                }
                if (clazz == null) {
                    return null;
                }
                constructor = clazz.getConstructor(mConstructorSignature);//拿到 构造方法，
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            constructor.setAccessible(true);//
            sConstructorMap.put(name, constructor);//然后缓存起来，下次再用，就直接从内存中去取
        }
        Object[] args = mConstructorArgs;
        args[1] = attrs;
        try {
            //通过反射创建View对象
            final View view = constructor.newInstance(args);//执行构造函数，拿到View对象
            return view;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //***********************************************************************************************************************************

}
