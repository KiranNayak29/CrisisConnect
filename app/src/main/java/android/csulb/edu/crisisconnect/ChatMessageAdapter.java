package android.csulb.edu.crisisconnect;

import android.content.Context;
import android.csulb.edu.crisisconnect.database.ChatHistoryContract;
import android.database.Cursor;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Created by vaibhavjain on 4/17/2017
 */

class ChatMessageAdapter extends CursorAdapter {
    private Context ctx;

    ChatMessageAdapter(Context ctx, Cursor cursor) {
        super(ctx, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.chat_message, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.messageView = (TextView) view.findViewById(R.id.single_message);
            holder.imageView = (ImageView) view.findViewById(R.id.chat_image);
            holder.layout = (LinearLayout) view.findViewById(R.id.chat_linear_layout);
            view.setTag(holder);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        String sender = cursor.getString(cursor.getColumnIndex(ChatHistoryContract.Messages.COLUMN_NAME_SENDER));
        int messageType = cursor.getInt(cursor.getColumnIndex(ChatHistoryContract.Messages.COLUMN_NAME_MESSAGE_TYPE));
        if (sender == null && messageType == 1) {
            holder.messageView.setBackgroundResource(R.drawable.bubble_receive);
            params.gravity = Gravity.START;
        } else if (sender != null && messageType == 1) {
            holder.messageView.setBackgroundResource(R.drawable.bubble_send);
            params.gravity = Gravity.END;
        } else if (sender == null && messageType == 2) {
            holder.imageView.setBackgroundResource(R.drawable.bubble_receive);
            params.gravity = Gravity.START;
        } else if (sender != null && messageType == 2) {
            holder.imageView.setBackgroundResource(R.drawable.bubble_send);
            params.gravity = Gravity.END;
        }
        holder.layout.setLayoutParams(params);
        if (messageType == 1) {
            holder.messageView.setText(cursor.getString(cursor.getColumnIndex(ChatHistoryContract.Messages.COLUMN_NAME_MESSAGE)));
            LinearLayout.LayoutParams params_message = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params_message.width = 0;
            params_message.height = 0;
            holder.imageView.setLayoutParams(params_message);
        } else if (messageType == 2) {
            holder.messageView.setBackgroundResource(0);
            holder.messageView.setText("");
            String path = cursor.getString(cursor.getColumnIndex(ChatHistoryContract.Messages.COLUMN_NAME_MESSAGE));
            //Glide lib abstracts reusing bitmap images and hence prevents OutOfMemoryException
            Glide.with(context).load(new File(path)).into(holder.imageView);
            LinearLayout.LayoutParams params_message = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params_message.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, context.getResources().getDisplayMetrics());
            params_message.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, context.getResources().getDisplayMetrics());
            ;
            holder.imageView.setLayoutParams(params_message);
        }
    }

    private class ViewHolder {
        TextView messageView;
        ImageView imageView;
        LinearLayout layout;
    }
}
