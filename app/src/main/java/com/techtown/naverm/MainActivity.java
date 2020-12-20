package com.techtown.naverm;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.graphics.PointF;
import android.location.Address;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.util.Log;

import static com.naver.maps.map.NaverMap.*;

public class MainActivity extends AppCompatActivity implements Overlay.OnClickListener, OnMapReadyCallback, OnCameraChangeListener, OnCameraIdleListener, OnMapClickListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private List<Marker> markerList = new ArrayList<Marker>();
    private boolean isCameraAnimated = false;
    private static final String TAG = "MainActivity";
    private InfoWindow infoWindow;
    private PointF pointF;
    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //네이버 지도 fragment 띄우

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {

            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        //FusedLocationSource 생성
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
    }

    //권한 요청
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부된 경우 트랙킹 모드 None
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    //지도 설정
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        naverMap.addOnCameraChangeListener(this);
        naverMap.addOnCameraIdleListener(this);
        naverMap.setOnMapClickListener(this);

        //infoWindow 띄우기
        infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultViewAdapter(this) {
            @NonNull
            @Override
            public View getContentView(@NonNull InfoWindow infoWindow) {
                Marker marker = infoWindow.getMarker();
                Place place = (Place) marker.getTag();
                View view = View.inflate(MainActivity.this, R.layout.view, null);
                ((TextView) view.findViewById(R.id.PJT_NAME)).setText(place.PJT_NAME);
                ((TextView) view.findViewById(R.id.PJT_DATE)).setText(place.PJT_DATE);
                ((TextView) view.findViewById(R.id.SITE_ADDR)).setText(place.SITE_ADDR);
                return view;
            }
        });

        LatLng mapCenter = naverMap.getCameraPosition().target;
        workPlace(mapCenter.latitude,mapCenter.longitude);

    }

    private void workPlace(double LAT, double LNG){

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://openapi.seoul.go.kr:8088")
                .addConverterFactory(GsonConverterFactory.create()).build();
        Api api = retrofit.create(Api.class);
        api.getPlacesByGeo(LAT,LNG).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {


                if (response.code()==200){
                    ApiResult apiResult = response.body();

                    updateMapMarkers(apiResult.getListConstructionWorkService());
                }
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                Log.d(TAG, t.getMessage());

            }
        });
    }

    private void updateMapMarkers(Places nplaces){
        resetMarkerList();
        if (nplaces.getRow() != null && nplaces.getRow().size()>0){
            for (Place place : nplaces.getRow()){
                Log.d("ks",place.SITE_ADDR);
                Marker marker = new Marker();
                marker.setTag(place);
                marker.setPosition(new LatLng(place.LAT, place.LNG));

                marker.setAnchor(new PointF(0.5f, 1.0f));
                marker.setMap(naverMap);
                marker.setOnClickListener(this);
                markerList.add(marker);
            }
        }
    }




    //
    @Override
    public boolean onClick(@NonNull Overlay overlay) {
        if (overlay instanceof Marker) {
            Marker marker = (Marker) overlay;
            if (marker.getInfoWindow() != null) {
                infoWindow.close();
            } else {
                infoWindow.open(marker);
            }
            return true;
        }
    return false;
    }

    private void resetMarkerList(){
        if (markerList != null && markerList.size()>0){
            for (Marker marker : markerList){
                marker.setMap(null);
            }
            markerList.clear();
        }
    }

    @Override
    public void onCameraChange(int reason, boolean animated) {
        isCameraAnimated = animated;

    }

    @Override
    public void onCameraIdle() {
        if (isCameraAnimated){
            LatLng mapCenter = naverMap.getCameraPosition().target;
            workPlace(mapCenter.latitude, mapCenter.longitude);
            }
        }


    //지도를 클릭하면 infoWindow 닫
    @Override
    public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
        if (infoWindow.getMarker() != null){
            infoWindow.close();
        }
    }


}