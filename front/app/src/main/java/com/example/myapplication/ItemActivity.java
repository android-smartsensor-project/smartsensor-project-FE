package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.ArrayList;

public class ItemActivity extends RecyclerView.Adapter<ItemActivity.ViewHolder> {

    private List<Item> activityList;

    public ItemActivity(List<Item> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = activityList.get(position);
        holder.typeTextView.setText(item.type);
        holder.detailTextView.setText(item.detail);
        holder.timeTextView.setText(item.time);
    }

    @Override
    public int getItemCount() {
        return activityList != null ? activityList.size() : 0;
    }

    public void addItem(Item item) {
        if (activityList == null) {
            activityList = new ArrayList<>();
        }
        activityList.add(0, item);
        notifyItemInserted(0);
    }

    public static class Item {
        public String type;
        public String detail;
        public String time;

        public Item(String type, String detail, String time) {
            this.type = type;
            this.detail = detail;
            this.time = time;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView typeTextView;
        public TextView detailTextView;
        public TextView timeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.textViewActivityType);
            detailTextView = itemView.findViewById(R.id.textViewActivitySummary);
            timeTextView = itemView.findViewById(R.id.textViewActivityTime);
        }
    }
}
