package com.bmt.bus_metro_timetracker;

import android.util.Xml;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiExplorer {
    String result = "데이터를 불러오는데 실패하였습니다.";
    ArrayList<String> busName;
    ArrayList<String> time1;
    public void get_response(String id){
        OkHttpClient client = new OkHttpClient();
        String arsId = id;
        String url = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByUid?ServiceKey=P0%2Boj4YjxjVqk0lXyBi9fZ%2B%2Fn1194GjEVVNHMsmh6tXGRfXihFgt8BX%2F%2BotYdkagyN3ASyAU8%2BeXuf6Ko%2BSDkA%3D%3D&arsId="+arsId;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String myResponse = response.body().string();
                    result = myResponse;
                }
                parseXML();
            }
        });
    }

    public void parseXML(){
        XmlPullParserFactory parserFactory;
        try{
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            InputStream is = new ByteArrayInputStream(result.getBytes());
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is,null);
            processParsing(parser);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
    private void processParsing(XmlPullParser parser) throws IOException, XmlPullParserException {
        busName = new ArrayList<>();
        time1 = new ArrayList<>();
        int eventType = parser.getEventType();
        String sw = null;
        while(eventType!=XmlPullParser.END_DOCUMENT){
            String name = "";
            switch (eventType){
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if("itemList".equals(name)){
                        sw = "not null";
                    }else if(sw != null){
                        if("rtNm".equals(name)){
                            busName.add(parser.nextText());
                        }else if("arrmsg1".equals(name)){
                            time1.add(parser.nextText());
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
    }
}