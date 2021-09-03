package com.zhihu.pdalbum;

import static com.xiaomi.mace.demo.result.InitData.DEVICES;
import static com.xiaomi.mace.demo.result.InitData.MODELS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.semantic.Semantic;
import com.example.testyuyinjni.TextUtil;
import com.xiaomi.mace.JniMaceUtils;
import com.xiaomi.mace.demo.AppModel;
import com.xiaomi.mace.demo.MaceApp;
import com.xiaomi.mace.demo.result.LabelCache;
import com.xiaomi.mace.demo.result.ResultData;

import java.io.File;
import java.nio.FloatBuffer;

public class ImgPresenter implements ImgContract.Presenter{

    private String storagePath;
    private String openclCacheFullPath;
    int openclCacheReusePolicy;
    int ompNumThreads;
    int cpuAffinityPolicy;
    int gpuPerfHint;
    int gpuPriorityHint;
    String model;
    String device;
    /**
     * storage rgb value
     */
    int[] colorValues;
    float[] inputColorValues;
    float[] classifyRes;
    String imgPath;

    String audioModelPath;
    String denoiseAudioPath;
    String sematicConfigPath;

    ResultData res;

    ClassifyCallBack classifyCallBack;
    AudioToTextCallBack audioToTextCallBack;
    SematicCallBack sematicCallBack;


    /**
     * mace float[] input
     */
    private FloatBuffer floatBuffer;

    /**
     * mace need data size width and height
     */
    private static final int FINAL_SIZE = 224;

    public ImgPresenter(ClassifyCallBack classifyCallBack,
                        AudioToTextCallBack audioToTextCallBack,
                        SematicCallBack sematicCallBack){
        this.classifyCallBack = classifyCallBack;
        this.audioToTextCallBack = audioToTextCallBack;
        this.sematicCallBack = sematicCallBack;

    }


    @Override
    public void classifyImg(String imgPath) {

        this.imgPath = imgPath;
        storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "mace";
        openclCacheFullPath  = storagePath + File.separator + "mace_cl_compiled_program.bin";
//        storagePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/storagePath";
//        openclCacheFullPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/openClCache";
        openclCacheReusePolicy = 1;  // 这几个数字是参考mace demo 的
        ompNumThreads = 2;
        cpuAffinityPolicy = 1;
        gpuPerfHint = 3;
        gpuPriorityHint = 3;
        model = MODELS[0];
        device = DEVICES[0];
        int gpuContext = JniMaceUtils.maceMobilenetCreateGPUContext(storagePath,openclCacheFullPath,openclCacheReusePolicy);
        int maceMobilenetEngine = JniMaceUtils.maceMobilenetCreateEngine(ompNumThreads,cpuAffinityPolicy,gpuPerfHint,gpuPriorityHint,model,device);

        convertImg2Float();
    }

    @Override
    public void convertImg2Float() {
        colorValues = new int[FINAL_SIZE * FINAL_SIZE];
        Bitmap bmp = getImage(imgPath);
        bmp.getPixels(colorValues, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

        float[] floatValues = new float[FINAL_SIZE * FINAL_SIZE * 3];
        floatBuffer = FloatBuffer.wrap(floatValues, 0, FINAL_SIZE * FINAL_SIZE * 3);

        handleColorRgbs();


    }

    @Override
    public void handleColorRgbs() {
        floatBuffer.rewind();
        for (int i = 0; i < colorValues.length; i++) {
            int value = colorValues[i];
            floatBuffer.put((((value >> 16) & 0xFF) - 128f)/ 128f);
            floatBuffer.put((((value >> 8) & 0xFF) - 128f) / 128f);
            floatBuffer.put(((value & 0xFF) - 128f) / 128f);
        }
        long start = System.currentTimeMillis();
        classifyRes = JniMaceUtils.maceMobilenetClassify(floatBuffer.array());

        final ResultData resultData = LabelCache.instance().getResultFirst(classifyRes);
        resultData.costTime = System.currentTimeMillis() - start;

        Log.d("MaceRes: ", "tesultData.name:"+ resultData.name+",probability: "+resultData.probability);

        classifyCallBack.notifyChange( resultData.name+","+resultData.probability);

    }

    /** 从缓存中获取图片 **/
    public Bitmap getImage(String imgPath) {
        final String path = imgPath;
        System.out.println("path:"+path);
        Bitmap bmp = BitmapFactory.decodeFile(path);
        //精确缩放到指定大小
        Bitmap thumbImg = Bitmap.createScaledBitmap(bmp,FINAL_SIZE,FINAL_SIZE, true);
        return thumbImg;
    }

    public interface ClassifyCallBack {
        void notifyChange(String data);
    }
    public interface AudioToTextCallBack {
        void onAudioToTextCallBack(String data);
    }
    public interface SematicCallBack {
        void onSematicCallBack(String data);
    }

    @Override
    public void audioDenoise(String audio) {
        denoiseAudioPath = audio + "_denoise";  // 这里没给audio加.pcm导致debug了很久qaq
        com.example.nslib.nsUtil.nsProcess(audio+".pcm",denoiseAudioPath+".pcm");
    }

    @Override
    public void audioToText(String audioPath) {
        audioDenoise(audioPath); // 这里不能加.pcm,不然等会audioDenoise里的文件名就有两个.pcm了
        String audioDenoisePath = audioPath + "_denoise";
        audioModelPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "audioModel"+ File.separator+"model.bin";
        String res = TextUtil.getText(audioModelPath,audioDenoisePath+".pcm");
        audioToTextCallBack.onAudioToTextCallBack(res);

    }

    @Override
    public void audioSematic(String audioPath) {
        sematicConfigPath = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"audioModel"+File.separator+"conf"+File.separator+"gramcel.config";
        String resSematic = Semantic.textToSemantic(sematicConfigPath,audioPath+"_denoise.pcm");
        Log.d("AudioSematic","返回值："+resSematic);
        sematicCallBack.onSematicCallBack(resSematic);

    }


}
