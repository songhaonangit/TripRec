package com.gc.triprec;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class PlaylistAdapter extends BaseAdapter {
    private List<String> m_filelist;
    private static final String TAG = "PlaylistAdapter";

    PlaylistAdapter(List<String> filelist, AdapterCallback callback) {
        super();
        m_filelist = filelist;
        m_callback = callback;
    }

    @Override
    public int getCount() {
        return m_filelist.size();
    }

    @Override
    public Object getItem(int position) {
        return m_filelist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    class ViewHolder {
        TextView m_tvname;
        public ViewHolder(View view) {
            m_tvname = (TextView) view.findViewById(R.id.file_name);
            view.setTag(this);
        }
    }

    public interface AdapterCallback {
        void startPlayback(String item);
    }

    private AdapterCallback m_callback;
}
