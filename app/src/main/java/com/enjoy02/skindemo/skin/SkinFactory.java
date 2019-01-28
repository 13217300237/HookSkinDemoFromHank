package com.enjoy02.skindemo.skin;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
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

    private AppCompatDelegate mDelegate;//委托?干啥的，先放着
    private List<SkinView> cacheSkinView = new ArrayList<>();//缓存所有可以换肤的View对象

    public void setDelegate(AppCompatDelegate mDelegate) {
        this.mDelegate = mDelegate;
    }

    static final Class<?>[] mConstructorSignature = new Class[]{
            Context.class, AttributeSet.class};// 反射构建构造方法的时候需要 传入 "形"参类型，这里Class数组里面有2个，一个是Context，一个是AttributeSet，
    // 这就对应了View的2个参数的构造函数 public View(Context context, @Nullable AttributeSet attrs) {...}
    final Object[] mConstructorArgs = new Object[2];//View的构造函数的2个"实"参对象

    private static final HashMap<String, Constructor<? extends View>> sConstructorMap =
            new HashMap<String, Constructor<? extends View>>();//用映射，将View的反射构造函数都存起来
    static final String[] prefixs = new String[]{
            "android.widget.",
            "android.view.",
            "android.webkit."
    };//安卓里面控件的包名，就这么3种,这个变量是为了下面代码里，反射创建类的class而预备的

    /**
     * 这是来自Factory的接口实现，我们这里要定义的是Factory2,所以这个方法返回空就行了
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
     * 经过反复对比，这段代码里面的内容，全都是来自
     * @param parent
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {

        // TODO: 关键点1：
        View view = mDelegate.createView(parent, name, context, attrs);//这句话没看懂？ 这一句话在系统源码里面是什么时候调用的，找到了,Ctrl+T,一下就跳过去了
        // 那么，delegate这个东西，在创建view的时候，又起了什么作用呢？
        // 4个参数，分别是 view的父view，view的name，上下文context，view的属性
        if (view == null) {//如果系统delegate委托类创建的是空的，那么 再由我自行创建一次,可是为什么delegate有可能创建空的呢？
            //卧槽，不看不知道，一看吓一跳，都是空。为啥？！
            Log.d("onCreateViewTag", name + "- null");
            mConstructorArgs[0] = context;
            try {//替代系统来创建view
                if (-1 == name.indexOf('.')) {//TextView
                    // 如果View的name中不包含 '.' 则说明是系统控件，会在接下来的调用链在name前面加上 'android.view.'
                    view = createViewByPrefix(context, name, prefixs, attrs);
                } else {
                    // 如果name中包含 '.' 则直接调用createView方法，onCreateView 后续也是调用了createView
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


    /**
     * TODO: 收集需要换肤的控件
     */
    private void collectSkinView(Context context, AttributeSet attrs, View view) {

        // 获取我们自己定义的属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Skinable);
        boolean isSupport = a.getBoolean(R.styleable.Skinable_isSupport, false);
        if (isSupport) {
            final int Len = attrs.getAttributeCount();
            HashMap<String, String> attrmap = new HashMap<>();
            for (int i = 0; i < Len; i++) {
                String attrName = attrs.getAttributeName(i);
                String attrValue = attrs.getAttributeValue(i);
                Log.i("Zero", "attrName: " + attrName + " attrValue: " + attrValue);
                attrmap.put(attrName, attrValue);
            }

            SkinView skinView = new SkinView();
            skinView.view = view;
            skinView.attrsMap = attrmap;
            cacheSkinView.add(skinView);
        }

    }

    public void changeSkin() {
        for (SkinView skinView : cacheSkinView) {
            skinView.changeSkin();
        }
    }

    static class SkinView {//这里为何要用静态类？难道对程序执行效率有影响,知识点：内部类 和 静态内部类由什么差别
        View view;
        HashMap<String, String> attrsMap;

        /**
         * TODO: 应用换肤
         */
        public void changeSkin() {
            if (!TextUtils.isEmpty(attrsMap.get("background"))) {//属性名,例如，这个background，text，textColor....
                int bgId = Integer.parseInt(attrsMap.get("background").substring(1));//属性值，R.id.XXX 这种，int类型
                String attrType = view.getResources().getResourceTypeName(bgId); // 属性类别：比如 drawable ,color
                if (TextUtils.equals(attrType, "drawable")) {
                    //这里说一下为什么要区分drawable和color，因为设置背景可能是纯色color，
                    // 也可能是drawable（这个单词可以理解为:可绘制的，可能是一张外界图片，也可能是drawableXXX.xml）
                    view.setBackgroundDrawable(SkinEngine.getInstance().getDrawable(bgId));//其实所谓换肤，就是 setXXX 外观相关属性的值
                } else if (TextUtils.equals(attrType, "color")) {
                    view.setBackgroundColor(SkinEngine.getInstance().getColor(bgId));
                }
            }

            if (view instanceof TextView) {
                if (!TextUtils.isEmpty(attrsMap.get("textColor"))) {
                    int textColorId = Integer.parseInt(attrsMap.get("textColor").substring(1));
                    String attrType = view.getResources().getResourceTypeName(textColorId);
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


}
