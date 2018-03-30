package suri.dakshi.com.stormy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather mCurrentWeather;

    @BindView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @BindView(R.id.timeLabel)
    TextView mTimeLabel;
    @BindView(R.id.locationLabel)
    TextView mLocationLabel;
    @BindView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @BindView(R.id.precipValue)
    TextView mPrecipValue;
    @BindView(R.id.humidityValue)
    TextView mHumidityValue;
    @BindView(R.id.iconImageView)
    ImageView mIconImageView;
    @BindView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mProgressBar.setVisibility(View.INVISIBLE);
        if (!isLocationAvailable()) {
            latitude = 37.8267;
            longitude = -122.4233;
        }
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
            }
        });
        getForecast(latitude, longitude);
    }

    private boolean isLocationAvailable() {

        boolean available = true;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
        else
        {
            available=false;
        }
        return available;
    }

    public void toggleRefresh()
    {
       if(mRefreshImageView.getVisibility()==View.VISIBLE)
       {
           mRefreshImageView.setVisibility(View.INVISIBLE);
           mProgressBar.setVisibility(View.VISIBLE);
       }
       else
       {
           mRefreshImageView.setVisibility(View.VISIBLE);
           mProgressBar.setVisibility(View.INVISIBLE);
       }
    }
    private void getForecast(double latitude,double longitude) {
        String apiKey="25d22cb5cc4ba5e87da16e79c98d0081";
        //"https://api.darksky.net/forecast/25d22cb5cc4ba5e87da16e79c98d0081/37.8267,-122.4233"
        String forecastUrl="https://api.darksky.net/forecast/" + apiKey + "/" + latitude + "," +longitude;
        if(isNetworkAvailable()) {

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData=response.body().string();
                        Log.e(TAG, jsonData);
                        if (response.isSuccessful()) {

                            mCurrentWeather=getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });

                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught", e);
                    }
                    catch (JSONException e)
                    {
                        Log.e(TAG, "Exception caught", e);
                    }
                }
            });
        }
        else
            Toast.makeText(this,"Network is unavailable",Toast.LENGTH_LONG).show();
    }

    private void updateDisplay() {

        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mLocationLabel.setText(mCurrentWeather.getTimeZone());
        mTimeLabel.setText("At "+mCurrentWeather.getFormattedTime()+" it will be");
        Drawable drawable=getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
        mHumidityValue.setText(mCurrentWeather.getHumidity()+"");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance()+" %");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {

        JSONObject forecast=new JSONObject(jsonData);
        String timezone=forecast.getString("timezone");
        Log.i(TAG,"From JSON "+timezone);
        //JSONObject minutely=forecast.getJSONObject("minutely");JSONArray arr=minutely.getJSONArray("data");JSONObject data=arr.getJSONObject(0);Long ans=data.getLong("time");Log.i(TAG,"Yeah  "+ans);
        JSONObject currently=forecast.getJSONObject("currently");
        CurrentWeather currentWeather=new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setTimeZone(timezone);
        Log.d(TAG,"Hello "+currentWeather.getFormattedTime());
        return currentWeather;
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager manager= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=manager.getActiveNetworkInfo();
        boolean isAvailable=false;
        if(networkInfo!=null && networkInfo.isConnected()) {
            isAvailable = true;
            toggleRefresh();
        }
        return isAvailable;
    }

    private void alertUserAboutError() {

    AlertDialogFragment dialog=new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error dialog");
    }
}
