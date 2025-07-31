package nemosofts.streambox.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;

import nemosofts.streambox.interfaces.GetMovieListener;
import nemosofts.streambox.item.ItemMovies;
import nemosofts.streambox.util.helper.JSHelper;

public class GetMoviesPlaylist extends AsyncTask<String, String, String> {

    private final JSHelper jsHelper;
    private final GetMovieListener listener;
    private final ArrayList<ItemMovies> itemMovies = new ArrayList<>();
    private final String catName;
    private final int page;
    private static final int ITEMS_PER_PAGE = 10;

    public GetMoviesPlaylist(Context ctx, int page, String catName, GetMovieListener listener) {
        this.listener = listener;
        this.catName = catName;
        this.page = page;
        jsHelper = new JSHelper(ctx);
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            ArrayList<ItemMovies> moviesList = new ArrayList<>(jsHelper.getMoviesPlaylist());

            for (ItemMovies movie : moviesList) {
                addOrUpdateItem(itemMovies, catName, movie);
            }

            if (jsHelper.getIsMovieOrder()) {
                Collections.reverse(itemMovies);
            }

            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, itemMovies.size());
            itemMovies.clear(); // Clear the list before adding paginated items
            for (int i = startIndex; i < endIndex; i++) {
                itemMovies.add(moviesList.get(i));
            }

            return "1";
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    private void addOrUpdateItem(ArrayList<ItemMovies> arrayList, String catName, ItemMovies movie) {
        if (movie != null && movie.getCatName().equals(catName)) {
            arrayList.add(movie);
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s,itemMovies);
        super.onPostExecute(s);
    }
}