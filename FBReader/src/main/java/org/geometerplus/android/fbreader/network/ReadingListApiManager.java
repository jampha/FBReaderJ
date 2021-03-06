package org.geometerplus.android.fbreader.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.bookshare.net.BookshareHttpOauth2Client;
import org.geometerplus.android.fbreader.network.bookshare.Bookshare_Webservice_Login;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by avanticatechnologies on 1/3/17.
 */

public class ReadingListApiManager {

    public static final String RESULT_SUCCESS = "RESULT_SUCCESS";

    private String accessToken = null;
    private AsyncTask pendingTask = null;
    private Context context;

    private static final ReadingListApiManager instance = new ReadingListApiManager();


    static public void createReadingList(Context context, String readingListName, ReadinglistAPIListener listener){
        instance._createReadingList(context, readingListName, listener);
    }
    public void _createReadingList(Context context, String readingListName, ReadinglistAPIListener listener){
        this.context = context;
        accessToken = null;
        CreateReadingListTask createTask = new CreateReadingListTask();
        createTask.readingListName = readingListName;
        createTask.listener = listener;
        runTask(createTask);

    }

    static public void removeFromReadingList(Context context, String listBookshareId, String bookBookshareId, ReadinglistAPIListener listener){
        instance._removeFromReadingList(context, listBookshareId, bookBookshareId, listener);
    }
    public void _removeFromReadingList(Context context, String listBookshareId, String bookBookshareId, ReadinglistAPIListener listener){
        this.context = context;
        accessToken = null;
        RemoveTitleFromReadingListTask removeTask = new RemoveTitleFromReadingListTask();
        removeTask.listBookshareId = listBookshareId;
        removeTask.bookId = bookBookshareId;
        removeTask.listener = listener;
        runTask(removeTask);

    }

    private void runTask(AsyncTask task){
        if(accessToken == null){
            pendingTask = task;
            new GetAuthenticatedTokenTask().execute();
        }
        else {
            task.execute();
        }
    }

    private void setToken(String token){
        accessToken = token;
        if(pendingTask != null){
            runTask(pendingTask);
            pendingTask = null;
        }
    }

    private class GetAuthenticatedTokenTask extends AsyncTask<Object, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(Object... params) {
            try {
                SharedPreferences login_preference = PreferenceManager.getDefaultSharedPreferences(context);
                String username = login_preference.getString(Bookshare_Webservice_Login.USER, "");
                String password = login_preference.getString(Bookshare_Webservice_Login.PASSWORD, "");

                BookshareHttpOauth2Client client =  new BookshareHttpOauth2Client();
                HttpsURLConnection urlConnection = client.createBookshareApiUrlConnection(username, password);

                String response = client.requestData(urlConnection);
                JSONObject jsonResponse = new JSONObject(response);
                String accessToken = jsonResponse.getString(BookshareHttpOauth2Client.ACCESS_TOKEN_CODE);

                return accessToken;
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String result) {
            setToken(result);
        }
    }

    private class CreateReadingListTask extends AsyncTask<Object, Void, Bundle> {
        public String readingListName;
        public ReadinglistAPIListener listener;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bundle doInBackground(Object... params) {
            try {
                BookshareHttpOauth2Client client =  new BookshareHttpOauth2Client();
                return client.postReadingList(accessToken, readingListName, "");
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
                return new Bundle();
            }
        }

        protected void onPostExecute(Bundle results) {
            boolean success = results.getBoolean(BookshareHttpOauth2Client.CODE_REQUEST_SUCCESS, false);
            results.putString(RESULT_SUCCESS, Boolean.toString(success));
            if(success){
                listener.onAPICallResult(results);
            }
            else {
                listener.onAPICallError(results);
            }
        }
    }

    class RemoveTitleFromReadingListTask extends AsyncTask<Object, Void, Boolean> {

        public ReadinglistAPIListener listener;
        String listBookshareId, bookId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Boolean doInBackground(Object... urls) {
            try {
                BookshareHttpOauth2Client client =  new BookshareHttpOauth2Client();
                return client.deleteTitleFromReadingList(accessToken, listBookshareId, bookId);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            Bundle results = new Bundle();
            results.putString(RESULT_SUCCESS, result.toString());
            if(result){
                listener.onAPICallResult(results);
            }
            else {
                listener.onAPICallError(results);
            }
        }
    }


    public interface ReadinglistAPIListener {
        void onAPICallResult(Bundle results);
        void onAPICallError(Bundle results);
    }
}
