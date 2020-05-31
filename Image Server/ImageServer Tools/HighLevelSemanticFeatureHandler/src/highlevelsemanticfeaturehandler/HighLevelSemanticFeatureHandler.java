/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package highlevelsemanticfeaturehandler;

import IRTEX_Exception.IRTEX_Exception;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fileUtils.FileUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import model.response;
import models.Score;
import stringutils.StringUtils;

/**
 *
 * @author SUBHAJIT
 */
public class HighLevelSemanticFeatureHandler {
    @Expose(serialize = true, deserialize = true)
    String name;
    @Expose(serialize = true, deserialize = true)
    public float[] feature;

    String URL;
    
    public static HashMap<String, HighLevelSemanticFeatureHandler> allHLSFHash;

    public HighLevelSemanticFeatureHandler(String name, String URL) {
        this.name = name;
        this.URL = URL;
        if(this.URL.endsWith("/"))
        {
            this.URL = StringUtils.replaceLast(this.URL, "/", "");
        }

    }

    public HighLevelSemanticFeatureHandler(String name, float[] feature) {
        this.name = name;
        this.feature = feature;

    }

    public void getFeature() throws IRTEX_Exception {
        if (URL == null) {
            throw new IRTEX_Exception(IRTEX_Exception.URLException);
        }
        
        String strURL = URL+"/segment";
        
        HttpResponse<String> str = Unirest.get(strURL).queryString("fileName", name).asString();

        if (str == null || str.getStatus() != 200) {
            throw new IRTEX_Exception(IRTEX_Exception.HighLevelFeatureExtractionFailure);
        }
        
        Type t = new TypeToken<response>(){}.getType();
        
        response resp = (response) FileUtils.loadGsonStringData(t, str.getBody());
        
        feature = resp.feature;
        
        
    }
    
    
    public static void extract_n_write(String fileName, String pathOfImg, String URL) throws IRTEX_Exception, FileNotFoundException, IOException
    {
        ArrayList<String> images = new ArrayList<>();
        FileUtils.listf(pathOfImg, images);
        
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        
        FileOutputStream out=new FileOutputStream(FileUtils.createFile(fileName));   
        
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();
        try
        {
            for(int i=0; i<images.size(); i++)
            {
                System.out.println(images.get(i));
                HighLevelSemanticFeatureHandler handler = new HighLevelSemanticFeatureHandler(images.get(i), URL);
                handler.getFeature();
                gson.toJson(handler, HighLevelSemanticFeatureHandler.class, writer);
            }
        }
        finally{
            writer.endArray();
            writer.close();
        }
        
    }
    
    
    public static HashMap<String, HighLevelSemanticFeatureHandler> load_HLSF_data_with_stream(String file) throws IRTEX_Exception, FileNotFoundException, IOException
    {
        Gson gson = new Gson();
        HighLevelSemanticFeatureHandler hslf;
        FileInputStream in=new FileInputStream(file);
        JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        allHLSFHash = new HashMap<>(50000);
        try{
            reader.beginArray();
            while (reader.hasNext()) 
            {
                hslf = gson.fromJson(reader, HighLevelSemanticFeatureHandler.class);
                System.out.println(hslf.name);
                allHLSFHash.put(hslf.name, hslf);
            }
            reader.endArray();
        }
        catch(Exception e){}
        finally{
            
            reader.close();
        }
        return allHLSFHash;
    }
    
    
    
    
    public void compare(String img, Score imgscr) throws IRTEX_Exception
    {
        HighLevelSemanticFeatureHandler baseIMG;
        float[] baseIMG_array;
        
        
        baseIMG = allHLSFHash.get(img);
        baseIMG_array = baseIMG.feature;
        if(baseIMG_array == null)
        {
            throw new IRTEX_Exception(IRTEX_Exception.ArrayNull);
        }
        
        imgscr.HLSFScore(img, (float) l2dist(feature, baseIMG_array));
        
        
    }
    
    
    static float l2dist(float[]a, float[]b)
    {
        float sum = 0;
        for(int i=0; i<a.length; i++)
        {
            sum+= Math.pow((a[i]-b[i]), 2);
        }
        return (float) Math.sqrt(sum);
    }

}
