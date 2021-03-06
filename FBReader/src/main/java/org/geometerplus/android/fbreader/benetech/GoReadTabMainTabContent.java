package org.geometerplus.android.fbreader.benetech;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.geometerplus.android.fbreader.library.DownloadedBookInfoActivity;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.ui.android.util.SortUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.geometerplus.android.fbreader.library.DownloadedBookInfoActivity.REQUEST_BOOK_INFO;

/**
 * Created by animal@martus.org on 4/26/16.
 */
public class GoReadTabMainTabContent extends ListFragment implements SortUtil.SortChangesListener{


    private ArrayList<AbstractTitleListRowItem> downloadedBooksList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        downloadedBooksList = new ArrayList<>();
        try {
            fillListAdapter();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        SortUtil.registerForSortChanges(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        SortUtil.unregisterForSortChanges(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == DownloadedBookInfoActivity.REQUEST_BOOK_INFO){
            if(resultCode == DownloadedBookInfoActivity.RESULT_BOOK_DELETED){
                fillListAdapter();
            }
        }
    }

    private void fillListAdapter() {
        if(getActivity() instanceof MyBooksActivity){
            HashMap<Long, Book> map;
            try{
                map = ((MyBooksActivity)getActivity()).getDownloadedBooksMap();
            }
            catch (Exception e){
                Log.e("GoReadTabMainController", "fill list adapter crashed", e);
                map = new HashMap<>();
            }
            for(Book book :map.values()){
                downloadedBooksList.add(new DownloadedTitleListRowItem(book));
            }
        }
        sortListItems();
        setListAdapter(new BookListAdapter(getActivity(), downloadedBooksList));
    }

    private void sortListItems() {
        Collections.sort(downloadedBooksList, SortUtil.getComparator());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        AbstractTitleListRowItem clickedRowItem = downloadedBooksList.get(position);
        Intent intent = new Intent(getActivity().getApplicationContext(), DownloadedBookInfoActivity.class);
        intent.putExtra(DownloadedBookInfoActivity.CURRENT_BOOK_PATH_KEY, clickedRowItem.getBookFilePath());
        startActivityForResult(intent, REQUEST_BOOK_INFO);
    }

    @Override
    public void onSortChanged(){
        sortListItems();
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onForceRefresh(){
        if(getActivity() != null) {
            downloadedBooksList.clear();
            fillListAdapter();
        }
    }


}
