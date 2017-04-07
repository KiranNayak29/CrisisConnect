package android.csulb.edu.crisisconnect;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by vaibhavjain on 4/6/2017
 */

public class NetworkListAdapter extends ArrayAdapter<ScanResult>{

    public NetworkListAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    public NetworkListAdapter(Context context, int resource, List<ScanResult> items){
        super(context, resource, items);
    }

    @NonNull
    @Override
    public View getView(int position,@NonNull  View convertView,@NonNull ViewGroup parent) {

        LayoutInflater inflater =(LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.network_listview_row, null);
        ScanResult client = getItem(position);
        if(client !=null){
            TextView textView = (TextView) v.findViewById(R.id.text_view_for_list);
            textView.setText(client.SSID);
        }
        return v;
    }
}


