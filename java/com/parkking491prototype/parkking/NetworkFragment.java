package com.parkking491prototype.parkking;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

//import javax.net.ssl.HttpsURLConnection;


/**
 * Implementation of headless Fragment that runs an AsyncTask to fetch data from the network.
 */
public class NetworkFragment extends Fragment {
    public static String TAG = "NetworkFragment";

    private static final String URL_KEY = "UrlKey";

    private DownloadCallback<String> mCallback;
    private DownloadTask downloadTask;
    private String urlString;

    private QueryType queryType;

    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, String tag, DownloadCallback<String>mCallback) {
        TAG = tag;
        NetworkFragment networkFragment = new NetworkFragment();
        //networkFragment.urlString = url;
        //Bundle args = new Bundle();
        //args.putString(URL_KEY, url);
        //networkFragment.setArguments(args);
        fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        networkFragment.setmCallback(mCallback);
        return networkFragment;
    }


    public void setmCallback(DownloadCallback<String>mCallback){
        this.mCallback = mCallback;
    }

    public void setUrlStringAndQueryType(QueryType qtype, String url)
    {
        urlString = url;
        queryType = qtype;
    }

    public QueryType getQueryType()
    {
        return queryType;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //urlString = getArguments().getString(URL_KEY);


        setRetainInstance(true);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Host Activity will handle callbacks from task.
//        mCallback = (DownloadCallback<String>) getActivity().getFragmentManager().findFragmentByTag("logdata");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity to avoid memory leak.
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelDownload();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask.
     */
    public void startDownload() {
        cancelDownload();
        downloadTask = new DownloadTask(mCallback);
        downloadTask.execute(urlString);
    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    public void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
    }


    /**
     * Implementation of AsyncTask designed to fetch data from the network.
     */
    private class DownloadTask extends AsyncTask<String, Integer, DownloadTask.Result> {

        private DownloadCallback<String> mCallback;

        DownloadTask(DownloadCallback<String> callback) {
            setCallback(callback);
        }

        void setCallback(DownloadCallback<String> callback) {
            mCallback = callback;
        }

        /**
         * Wrapper class that serves as a union of a result value and an exception. When the download
         * task has completed, either the result value or exception can be a non-null value.
         * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
         */
        class Result {
            public String mResultValue;
            public Exception mException;
            public Result(String resultValue) {
                mResultValue = resultValue;
            }
            public Result(Exception exception) {
                mException = exception;
            }
        }

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo networkInfo = mCallback.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected() ||
                        (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                                && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                    // If no connectivity, cancel task and update Callback with null data.
                    mCallback.updateFromDownload(null, null);
                    cancel(true);
                }
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected DownloadTask.Result doInBackground(String... urls) {
            Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    String resultString = downloadUrl(url);
                    if (resultString != null) {
                        result = new Result(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                } catch(Exception e) {
                    result = new Result(e);
                }
            }
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(Result result) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.updateFromDownload(queryType ,result.mException.getMessage());
                }
                if (result.mResultValue != null) {
                    mCallback.updateFromDownload(queryType ,result.mResultValue);
                }
                mCallback.finishDownloading(queryType);
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(Result result) {
            System.out.println("CANCELLED");
        }

        /**
         * Given a URL, sets up a connection and gets the HTTP response body from the server.
         * If the network request is successful, it returns the response body in String form. Otherwise,
         * it will throw an IOException.
         */
        private String downloadUrl(URL url) throws IOException {
            InputStream stream = null;
            HttpURLConnection connection = null;
            String result = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                //connection.setRequestProperty("parkinglot_ID", "491");


                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                //connection.setDoOutput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
                publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_ACCEPTED) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                // Retrieve the response body as an InputStream.
                int contentLength = connection.getContentLength();
                stream = connection.getInputStream();
                publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
                if (stream != null) {
                    // Converts Stream to String with max length of 500.
                    result = readStream(stream, contentLength);
                    System.out.println("RESULT:" + result);
                }
            } finally {
                // Close Stream and disconnect HTTP connection.
                if (stream != null) {
                    stream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return result;
        }

        /**
         * Converts the contents of an InputStream to a String.
         */
        public String readStream(InputStream stream, int maxReadSize)
                throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            char[] rawBuffer = new char[maxReadSize];
            int readSize;
            StringBuffer buffer = new StringBuffer();
            while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                if (readSize > maxReadSize) {
                    readSize = maxReadSize;
                }
                buffer.append(rawBuffer, 0, readSize);
                maxReadSize -= readSize;
            }
            return buffer.toString();
        }
    }


}