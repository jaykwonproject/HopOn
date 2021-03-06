package com.bmt.bus_metro_timetracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    Double lat, lon;
    boolean sw = false;
    @Nullable
    private LocationManager locationManager;
    private FusedLocationProviderClient mFLPC;
    ArrayList<String> busstopsNMs = new ArrayList<String>();
    ArrayList<String> buspoints = new ArrayList<String>();
    ArrayList<String> busstopIDs = new ArrayList<String>();
    HashMap<String,ArrayList<String>> busStopCords = new HashMap<String,ArrayList<String>>();
    boolean clicked = false;
    InfoWindow infoWindow = new InfoWindow();
    double xlondouble = 0.0;
    double ylatdouble = 0.0;
    int x = 0;
    ApiExplorer api = new ApiExplorer();
    SubwayExplorer subway = new SubwayExplorer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}
        setContentView(R.layout.activity_main);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_popup);
        linearLayout.setVisibility(View.GONE);
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mFLPC = LocationServices.getFusedLocationProviderClient(this);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
    int option = 0;
    String subwayNM ="";
    String finalKoreName="";
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        checkGPS();
        currentLocation();
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setLocationButtonEnabled(false);
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true);
        readstationID();
        TextView text = findViewById(R.id.stationInput);
        naverMap.setOnMapClickListener((point, coord) ->{
            lat = coord.latitude;
            lon = coord.longitude;
            if(sw!=false){
                infoWindow.close();
                infoWindow.setMap(null);
                sw = false;
            }
            showinfoWindow();

            String arsId = busstopIDs.get(x);
            api.get_response(arsId);
            option = 1;
        });

        naverMap.setOnSymbolClickListener(symbol -> {
            if (symbol.getCaption().contains("역")) {
                lat = symbol.getPosition().latitude;
                lon = symbol.getPosition().longitude;
                naverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(lat,lon)));
                option = 2;
                String subName = symbol.getCaption();
                String koreName = subName.replaceAll("[a-zA-Z]+", "");
                koreName = koreName.replaceAll("\\W", "");
                String[] finalName;
                finalName = koreName.split("역");
                finalKoreName = koreName;
                if(finalKoreName.contains("역")){
                    text.setText(finalKoreName);
                }else{
                    text.setText(finalKoreName+"역");
                }
                if(sw!=false){
                    infoWindow.close();
                    infoWindow.setMap(null);
                    sw = false;
                }
                subwayinfo(finalKoreName);

                if(finalKoreName.charAt(0)!='역'){
                    if(finalKoreName.matches("동대문역사문화공원역")) {
                        subway.get_response("동대문역사문화공원");
                    }else if(Character.toString(finalKoreName.charAt(0)).matches("^[0-9].*")){
                        subway.get_response(subwayName(finalName[0].substring(1)));
                    }
                    else{
                        subway.get_response(subwayName(finalName[0]));
                    }
                }
                else{
                    subway.get_response(subwayName('역'+finalName[1]));
                }
                subwayNM ="";
                return true;
            }
            return false;
        });
    }
    public String subwayName(String name) {
        ArrayList<String> storeNM = new ArrayList<>();
        InputStream is = getResources().openRawResource(R.raw.subwayname);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("x-windows-949")));
        String line = "";
        try{
            reader.readLine();
            while( (line = reader.readLine())!=null){
                String[] tokens = line.split(",");
                storeNM.add(tokens[2]);
            }
        } catch (IOException e){
            Log.wtf("MyActivity", "Error reading data file on line"+ line, e);
            e.printStackTrace();
        }
        int bol = 0;
        for(int i=0; i<storeNM.size(); i++){
            if(storeNM.get(i).matches(name)){
                subwayNM = storeNM.get(i);
                bol = 1;
            } else if(bol == 0 && storeNM.get(i).contains(name)){
                subwayNM = storeNM.get(i);
            }
        }
        return subwayNM;
    }

    public void subwayinfo(String name){
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return name;
            }
        });
        infoWindow.setPosition(new LatLng(lat, lon));
        infoWindow.open(naverMap);
        sw = true;
    }

    public void showinfoWindow(){
        TextView text = findViewById(R.id.stationInput);
        for (int i = 0; i < busstopsNMs.size(); i++) {
            String busxy = buspoints.get(i);
            String xy [] = busxy.split(",");

            String xlon = xy[0];
            String ylat = xy[1];

            xlondouble = Double.parseDouble(xlon);
            ylatdouble = Double.parseDouble(ylat);

            if(Math.abs(lon-xlondouble)<0.000185 && Math.abs(lat-ylatdouble)<0.000185){
                text.setText(busstopsNMs.get(i));
                int finalI = i;
                infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
                    @NonNull
                    @Override
                    public CharSequence getText(@NonNull InfoWindow infoWindow) {
                        return busstopsNMs.get(finalI);
                    }
                });
                infoWindow.setPosition(new LatLng(ylatdouble, xlondouble));
                infoWindow.open(naverMap);
                naverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(ylatdouble,xlondouble)));
                sw = true;
                clicked = true;
                x = finalI;
            }
        }

    }

    public void readstationID() {
        InputStream is = getResources().openRawResource(R.raw.data);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("x-windows-949")));
        String line = "";
        try{
            reader.readLine();
            while( (line = reader.readLine())!=null){
                String[] tokens = line.split(",");
                String coords = tokens[3]+","+tokens[4];
                ArrayList<String> points = new ArrayList<String>();
                busstopsNMs.add(tokens[2]);
                buspoints.add(coords);
                busstopIDs.add(tokens[1]);
                if (busStopCords.containsKey(tokens[2])) {
                    points = busStopCords.get(tokens[2]);
                }
                points.add(coords);
                busStopCords.put(tokens[2],points);
            }
        } catch (IOException e){
            Log.wtf("MyActivity", "Error reading data file on line"+ line, e);
            e.printStackTrace();
        }
    }



    String lineN = "";
    public String subwayLine(String id) {
        InputStream is = getResources().openRawResource(R.raw.linenumber);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("x-windows-949")));
        String line = "";
        try{
            reader.readLine();
            while( (line = reader.readLine())!=null){
                String[] tokens = line.split(",");
                if(tokens[0].contains(id)){
                    lineN = tokens[1];
                }
            }
        } catch (IOException e){
            Log.wtf("MyActivity", "Error reading data file on line"+ line, e);
            e.printStackTrace();
        }
        return lineN;
    }

    private RecyclerAdapter adapter;
    public void confirmOnclick(View view) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_popup);
        Button locationBtn = (Button) findViewById(R.id.locationBtn);
        locationBtn.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        if(option == 1){
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(linearLayoutManager);
            adapter = new RecyclerAdapter();
            recyclerView.setAdapter(adapter);

            ArrayList<String> name = api.busName;
            ArrayList<String> time = api.time1;
            //오류시
            if(sw==false){
                Data data = new Data();
                data.setTitle("불러오기 실패");
                data.setContent("불러오기 실패");
            }
            else{
                for(int i=0; i<name.size(); i++){
                    Data data = new Data();
                    data.setTitle(name.get(i));
                    data.setContent(time.get(i));
                    adapter.addItem(data);
                }
                adapter.notifyDataSetChanged();
            }
        }
        else if(option ==2){

            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(linearLayoutManager);
            SubwayAdapter adapter = new SubwayAdapter();
            recyclerView.setAdapter(adapter);
            ArrayList<String> subwayId = subway.subwayId;
            ArrayList<String> time1 = subway.time1;
            ArrayList<String> direction = subway.direction;
            ArrayList<String> currentStation = subway.currentStation;
            ArrayList<Integer> check = new ArrayList<>(subwayId.size());
            ArrayList<String> test = new ArrayList<>(subwayId.size());
            for(int x=0; x<subwayId.size(); x++){
                check.add(x,0);
            }

            for(int i=0; i<subwayId.size(); i++){
                if(check.get(i)==0){
                    for(int j=i; j<subwayId.size(); j++) {
                        if (check.get(j) == 0) {
                            if (subwayId.get(i).contains(subwayId.get(j))) {
                                check.set(j, 1);
                                Data data = new Data();
                                data.setTitle(subwayLine(subwayId.get(j)) + " ");
                                data.setSubTitle("[" + direction.get(j) + "]");
                                if (Integer.parseInt(time1.get(j)) / 60 < 1) {
                                    data.setContent("곧 도착");
                                } else {
                                    data.setContent(Integer.parseInt(time1.get(j)) / 60+"분");
                                }
                                //운행이 종료되면 에러 index 3 out of 3에러
                                //data.setContent1("[현재: "+currentStation.get(j)+"역]");

                                test.add(direction.get(j));

                                int ch = 0;
                                for (int y = j + 1; y < subwayId.size(); y++) {
                                    data.setContent1(", " + Integer.parseInt(time1.get(y)) / 60 + "분후 도착");
                                    check.set(y, 1);
                                    ch = 1;
                                }
                                if(ch==0){
                                    data.setContent1("후 도착");
                                }
                                adapter.addItem(data);
                            }
                        }
                    }
                    Data data = new Data();
                    adapter.addItem(data);
                }
            }
            adapter.notifyDataSetChanged();
        }
        else{
            Toast.makeText(this, "정류소 혹 역 먼저 선택해주세요", Toast.LENGTH_SHORT).show();
        }

    }


    public void exitBtnClicked(View view) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_popup);
        Button locationBtn = (Button) findViewById(R.id.locationBtn);
        locationBtn.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.removeAllViews();
    }

    public void checkGPS() {
        LocationManager mLocMan; // 위치 관리자
        mLocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            gps_enabled = true;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                            gps_enabled = false;
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermission()) {
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                    this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        /*
        if (naverMap == null || location == null) {
            return;
        }
        LatLng coord = new LatLng(location);
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);
        locationOverlay.setPosition(coord);
        locationOverlay.setBearing(location.getBearing());
        naverMap.moveCamera(CameraUpdate.scrollTo(coord));
        */
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private boolean hasPermission() {
        return PermissionChecker.checkSelfPermission(this, PERMISSIONS[0])
                == PermissionChecker.PERMISSION_GRANTED
                && PermissionChecker.checkSelfPermission(this, PERMISSIONS[1])
                == PermissionChecker.PERMISSION_GRANTED;
    }

    public void onLastLocationButtonClicked(View view) {
        currentLocation();
    }
    boolean gps_enabled = false;
    public void currentLocation(){
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        catch (Exception ex) {
        }
        if (gps_enabled != true) {
            checkGPS();
        }
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mFLPC.getLastLocation().addOnSuccessListener(this, location -> {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    CameraUpdate cameraUpdate = CameraUpdate.toCameraPosition(new CameraPosition(new LatLng(lat, lon), 17));
                    naverMap.moveCamera(cameraUpdate);
                });
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                mFLPC.getLastLocation().addOnSuccessListener(this, location -> {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    naverMap.setCameraPosition(new CameraPosition(new LatLng(lat, lon), 17));
                });
            }
        }
    }

}