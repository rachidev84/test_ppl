package nemosofts.streambox.interfaces;

import java.util.ArrayList;

import nemosofts.streambox.item.ItemLive;

public interface GetLiveListener {
    void onStart();
    void onEnd(String success, ArrayList<ItemLive> arrayListLive);
}