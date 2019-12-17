package com.lnstow.filemonitor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

public class StringAdapter extends RecyclerView.Adapter<StringAdapter.StringViewHolder> {
    private String[] data;
    private int len;
    private int color;

    public void setData(String[] data) {
        this.data = data;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String[] getData() {
        return data;
    }

    public StringAdapter(String[] data, int color) {
        this.data = data;
        len = 0;
        this.color = color;
    }

    @NonNull
    @Override
    public StringViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_file_info, parent, false);
        ((AppCompatTextView) view).setTextIsSelectable(true);
        ((AppCompatTextView) view).setHorizontallyScrolling(true);
//        ((AppCompatTextView) view).setTextColor(color);
        StringViewHolder holder = new StringViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull StringViewHolder holder, int position) {
//        Log.d("123123", "onBindViewHolder: "+len);
//        Log.d("123123", FileListener.number + "  " + FileListener.FullInfo[position]);
//        Log.d("123123", position+"  "+data[position]);
        holder.textView.setTextColor(color);
        holder.textView.setText(data[position]);
    }

    @Override
    public int getItemCount() {
        return len;
    }

    static class StringViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView textView;

        public StringViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (AppCompatTextView) itemView;
        }
    }

}
