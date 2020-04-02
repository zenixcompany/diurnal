package com.example.calendar;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.calendar.models.Record;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
    private ArrayList<Record> recordList;
    private HashMap<String, Integer> dayNamesColors;

    private RecordsListener listener;

    private Calendar calendar = Calendar.getInstance();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;

        public ViewHolder(CardView v) {
            super(v);
            cardView = v;
        }
    }

    public RecordsAdapter(ArrayList<Record> recordList) {
        this.recordList = recordList;
        initColors();
    }

    public static interface RecordsListener {
        public void onClick(int position);
    }

    public void setListener(RecordsListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cv = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_record,
                                                     parent, false);

        return new ViewHolder(cv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardView cardView = holder.cardView;

        TextView dayName = cardView.findViewById(R.id.date_day_text);
        TextView dayNumber = cardView.findViewById(R.id.date_day_number);
        TextView month = cardView.findViewById(R.id.date_month);

        calendar.setTime(recordList.get(position).getDate());
        String dayNameStr = Objects.requireNonNull(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
        dayName.setText(dayNameStr.toUpperCase());

        dayName.setBackgroundColor(dayNamesColors.get(dayNameStr));

        dayNumber.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        month.setText(Objects.requireNonNull(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)).toUpperCase());

        TextView recordTitle = cardView.findViewById(R.id.record_title);
        TextView recordText = cardView.findViewById(R.id.record_text);

        recordTitle.setText(recordList.get(position).getTitle());
        recordText.setText(recordList.get(position).getText());

        cardView.setOnClickListener(view -> {
            if (listener != null)
                listener.onClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public void addRecord(Record record) {
        // TODO Try to make this shit filtered explicitly by RecyclerView filter
        recordList.add(0, record);
    }

    public void updateRecord(int position, Record record) {
        recordList.get(position).setTitle(record.getTitle());
        recordList.get(position).setText(record.getText());

        Record toDelete = recordList.get(position);
        recordList.remove(position);

        recordList.add(0, toDelete);

        notifyDataSetChanged();
    }

    public void deleteRecord(int position) {
        recordList.remove(position);

        notifyDataSetChanged();
    }

    private void initColors() {
        dayNamesColors = new HashMap<>();
        dayNamesColors.put("Mon", Color.parseColor("#FF8A80"));
        dayNamesColors.put("Tue", Color.parseColor("#FF80AB"));
        dayNamesColors.put("Wed", Color.parseColor("#B388FF"));
        dayNamesColors.put("Thu", Color.parseColor("#8C9EFF"));
        dayNamesColors.put("Fri", Color.parseColor("#B9F6CA"));
        dayNamesColors.put("Sat", Color.parseColor("#FFD180"));
        dayNamesColors.put("Sun", Color.parseColor("#FFFF8D"));
    }
}
