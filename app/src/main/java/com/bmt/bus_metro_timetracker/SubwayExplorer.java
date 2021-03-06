package com.bmt.bus_metro_timetracker;

import org.jetbrains.annotations.NotNull;
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

public class SubwayExplorer {
    String result = "데이터를 불러오는데 실패하였습니다.";
    ArrayList<String> subwayId;
    ArrayList<String> time1;
    ArrayList<String> direction;
    ArrayList<String> currentStation;
    public void get_response(String name){
        OkHttpClient client = new OkHttpClient();
        String url = "http://swopenapi.seoul.go.kr/api/subway/744d52747368797537376f49417044/xml/realtimeStationArrival/0/20/"+name;
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
        subwayId = new ArrayList<>();
        time1 = new ArrayList<>();
        direction = new ArrayList<>();
        currentStation = new ArrayList<>();
        int eventType = parser.getEventType();
        String sw = null;
        while(eventType!=XmlPullParser.END_DOCUMENT){
            String name = "";
            switch (eventType){
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if("row".equals(name)){
                        sw = "not null";
                    }else if(sw != null){
                        if("subwayId".equals(name)){
                            subwayId.add(parser.nextText());
                        }else if("barvlDt".equals(name)){
                            time1.add(parser.nextText());
                        }else if("trainLineNm".equals(name)){
                            direction.add(parser.nextText());
                        }else if("arvlMsg3".equals(name)){ //이게없으면 운행종료
                            currentStation.add(parser.nextText());
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
    }
}
