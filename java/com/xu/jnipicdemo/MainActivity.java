package com.xu.jnipicdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.mt.mtxx.image.JNI;

/*这个用的是别人的so包
*运行的时候和c文件没有任何关系的，打包的时候c不会打进去，只不过在编译的阶段会起作用，真正起作用的是so文件
*
* so包应该放在相应模块（比如app模块）下的src目录下的main目录下的jniLibs目录。
注意是jniLibs，最后边有个s，不是jniLib，并且L要大写。如果你在src/main目录中看不到jniLibs目录，
那你只需要自己建一个这个目录就可以了，然后把你的so包按编译平台分类拷贝进去就可以了。然后呢？然后就完了，
就这样就可以了，因为系统默认就会去这个目录中找对应的so包。如下图所示：
*
*
* 原理：
其实在AndroidStudio中，我们之所以可以把jar包放在对应模块的libs目录下，
比如app模块（也就是通常意义下的主工程了）的libs目录中，而不需要再去配置build.gradle文件，就可以正常使用这些jar包，
是因为在AndroidStudio中新建项目时，系统已经在默认为我们配置好了gradle，如果仔细看模块下的build.gradle文件的话，一般会有这么一句：

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
}


看到了吧，这句就是告诉gradle，我们的第三方jar包在libs目录下，如果没有这一行配置，
那么理论上我们把jar包直接放在libs目录下其实是不行的，只不过新建项目时系统已经为我们配好了。
这里libs是个相对路径，因为我们的build.gradle文件本身就在app模块下，那么这里的libs当然也就是指app模块下的libs，
其实build.gradle文件中所有的路径都可以写相对路径，我们下边讲的so包的路径也是只写相对路径就可以了。

OK，言归正传，现在知道jar包的引入原理了，那么so包为什么要放在那么奇怪的目录下？能不能放在任意一个自定义的目录呢？当然是可以的。

其实在build.gradle中，默认会有一些这样的配置：

sourceSets {
    main {
        jniLibs.srcDirs = ['src/main/jniLibs']
        aidl.srcDirs = ['src/main/aidl']
    }
}

这些配置在你新建工程之后，在build.gradle文件中默认是没有的，因为默认就是这样的，当然也就不写也可以，当然你写出来也没错。
意思是so包就去src/main/jniLibs目录下找，aidl文件默认就到src/main/aidl目录下找。。。当然还有许多其它的配置，
你还可以配置你的AndroidManifest.xml文件在哪里，还可以配置你的Java代码在哪里，如果你不配的话都会有一个默认值，这里只是以jniLibs和aidl为例而已。

看到这里你应该就明白了，为什么我们把so包直接放到src目录下的main目录下的jinLibs目录就可以了，而不需要配置gradle文件了，
因为系统默认就会到这个目录下找，如果你想把so包放在一个你喜欢的地方，比如直接放在app目录下的myJniLibs目录下，那你只需要把上面内容改成：

sourceSets {
    main {
        jniLibs.srcDirs = ['myJniLibs']
        aidl.srcDirs = ['src/main/aidl']
    }
}

这样就可以了。明白了原理，似乎一切都明了了，同理，如果不想把aidl文件放在默认目录下，也可以改aidl.srcDirs的值，
如果不想把java代码放在默认目录，就可以改java.srcDirs的值，如果不想把资源文件放在默认目录下，就可以改res.srcDirs的值。。。还有几个，
同学们慢慢去研究吧，如果你想把目录结构改的跟eclipse下的工程一样，就改这里的值就可以，不一一细说了。
* */
public class MainActivity extends AppCompatActivity {
    private ImageView image;
    //直接放在这个包里是不行的，因为jni本地方法名和c的函数名有一套对应规则，
    // c的函数名是java_包名类名本地方法名，现在so是别人的，他的包名是不一样的，所以不能调用
    private JNI jni;
    private Bitmap bm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //找到图片的控件
        image = (ImageView) findViewById(R.id.image);
        //加载图片
        bm = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
        //将图片设置到控件上。显示
        image.setImageBitmap(bm);
        jni = new JNI();
    }

    //点击按钮调用jni的图片处理方法
    public void process(View v){
        //图片的宽和高，这里拿到了bitmap的宽高
        //获取这个图片的宽度是一行有多少个像素
        int width = bm.getWidth();
        //获取这个图片的宽度是一列有多少个像素
        int height = bm.getHeight();
        //创建一个数组用来存放bitmap的像素，bitmap有多少像素就要创建相应大小的数组（整张图片就是高乘以宽这么像素点组成）所以用一个数组保存所有的像素信息
        int[] pixels = new int[width*height];
        //接收一个int类型的数组，一张图片可以转化成像素的数组，创建一个数组
        //argb 8888  alpha red green blue 8 8 8 8（8位的二进制数） 32位2进制数 int是四个字节。用一个int类型的变量就可以表示一个像素点的像素信息保存
        // （图片所支持）的格式，安卓可以支持最高清晰度图片的格式
        //参数：1用来接收图片颜色的数组，2写进去像素的第一个偏移量处理整张图片不偏移0，3图片逐行扫描的信息，一般大于等于图片的宽度，
        // 4从bitmap读的第一个像素坐标，读出整张图片来，就从头开始读，x的坐标，5是y坐标，
        // 6图片的宽度，7图片的高度传进去
        //getpixels运行后 pixels数组就被修改了 pixels中保存了bm的颜色信息
        bm.getPixels(pixels, 0, width, 0, 0, width, height);

        //StyleLomoB 运行后 pixels的颜色信息已经被修改 修改成特效处理后的颜色信息
        //pixels就包含了所有的颜色信息，宽度和高度都传进去，调用这个方法就修改了这个pixels
        jni.StyleBaoColor(pixels, width, height);
        //用处理好的像素数组 创建一张新的图片 这张图片就是经过特效处理的
        //第一个参数是数组处理好的数组传进去，还是原图的宽高不需要处理，配置信息还是原来的bitmap
        Bitmap bm2 = Bitmap.createBitmap(pixels, width, height, bm.getConfig());
        //处理好的图片在显示到控件上面
        image.setImageBitmap(bm2);
    }
}
