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

public class StationExplorer {
    String result = "데이터를 불러오는데 실패하였습니다.";
    String arsId;
    String stationNM;
    ArrayList<String> idNname = new ArrayList<>();
    public ArrayList<String> get_response(double x, double y){
        OkHttpClient client = new OkHttpClient();
        double xpos = x;
        double ypos = y;
        String url = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByPos?ServiceKey=P0%2Boj4YjxjVqk0lXyBi9fZ%2B%2Fn1194GjEVVNHMsmh6tXGRfXihFgt8BX%2F%2BotYdkagyN3ASyAU8%2BeXuf6Ko%2BSDkA%3D%3D&tmX="+xpos+"&tmY="+ypos+"&radius=20";
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
        idNname.add(arsId);
        idNname.add(stationNM);
        return idNname;
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
        arsId = "";
        stationNM = "";
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
                        if("arsId".equals(name)){
                            arsId = parser.nextText();
                        }else if("stationNm".equals(name)){
                            stationNM = parser.nextText();
                        }
                    }
                    break;
            }
            eventType = parser.next();
            System.out.println(arsId+" and "+stationNM);
        }
    }
}
