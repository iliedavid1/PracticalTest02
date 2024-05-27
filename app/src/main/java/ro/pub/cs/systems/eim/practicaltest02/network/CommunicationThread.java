package ro.pub.cs.systems.eim.practicaltest02.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import ro.pub.cs.systems.eim.practicaltest02.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.WeatherForecastInformation;

public class CommunicationThread extends Thread {
    private final ServerThread serverThread;
    private final Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");
            String city = bufferedReader.readLine();
            String informationType = bufferedReader.readLine();

            if (city == null || city.isEmpty() || informationType == null || informationType.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }

            HashMap<String, WeatherForecastInformation> data = serverThread.getData();
            WeatherForecastInformation weatherForecastInformation;
            if (data.containsKey(city)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                weatherForecastInformation = data.get(city);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                String url = Constants.WEB_SERVICE_ADDRESS + "?q=" + city + "&appid=" + Constants.WEB_SERVICE_API_KEY;
                URL urlAddress = new URL(url);
                URLConnection urlConnection = urlAddress.openConnection();
                BufferedReader bufferedReader1 = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String pageSourceCode;
                StringBuilder stringBuilder = new StringBuilder();
                String currentLine;
                while ((currentLine = bufferedReader1.readLine()) != null) {
                    stringBuilder.append(currentLine);
                }
                bufferedReader1.close();
                pageSourceCode = stringBuilder.toString();
                JSONObject content = new JSONObject(pageSourceCode);
                JSONArray weatherArray = content.getJSONArray("weather");

                JSONObject weather;
                StringBuilder condition = new StringBuilder();
                for (int i = 0; i < weatherArray.length(); i++) {
                    weather = weatherArray.getJSONObject(i);
                    condition.append(weather.getString("main")).append(":").append(weather.getString("description"));

                    if (i < weatherArray.length() - 1) {
                        condition.append(";");
                    }
                }

                JSONObject main = content.getJSONObject("main");
                String temperature = main.getString("temp");
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");

                JSONObject wind = content.getJSONObject("wind");
                String windSpeed = wind.getString("speed");

                weatherForecastInformation = new WeatherForecastInformation(
                        temperature, windSpeed, condition.toString(), pressure, humidity
                );
                serverThread.setData(city, weatherForecastInformation);
            }
            if (weatherForecastInformation == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }
            String result;
            switch (informationType) {
                case "all":
                    result = weatherForecastInformation.toString();
                    break;
                case "temperature":
                    result = weatherForecastInformation.getTemperature();
                    break;
                case "wind_speed":
                    result = weatherForecastInformation.getWindSpeed();
                    break;
                case "condition":
                    result = weatherForecastInformation.getCondition();
                    break;
                case "pressure":
                    result = weatherForecastInformation.getPressure();
                    break;
                case "humidity":
                    result = weatherForecastInformation.getHumidity();
                    break;
                default:
                    result = "Wrong information type (temperature / wind_speed / condition / pressure / humidity)!";
            }
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + e.getMessage());
            }
        }
    }

}
