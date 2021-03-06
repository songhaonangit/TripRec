package com.gc.triprec;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class PlaylistAdapter extends BaseAdapter {
    private List<File> m_filelist;
    private static final String TAG = "PlaylistAdapter";

    PlaylistAdapter(List<File> filelist, AdapterCallback callback) {
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(parent.getContext().getApplicationContext(), R.layout.fileitem, null);
            new ViewHolder(convertView);
        }
        ViewHolder holder = (ViewHolder) convertView.getTag();
        File item = (File)getItem(position);
        holder.m_tvname.setText(item.getName());
        holder.m_tvname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != m_callback)
                    m_callback.startPlayback(position);
            }
        });
        return convertView;
    }

    class ViewHolder {
        TextView m_tvname;
        public ViewHolder(View view) {
            m_tvname = view.findViewById(R.id.file_name);
            view.setTag(this);
        }
    }

    public interface AdapterCallback {
        void startPlayback(int position);
    }

    private AdapterCallback m_callback;
}
