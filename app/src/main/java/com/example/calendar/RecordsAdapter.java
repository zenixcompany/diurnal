package com.example.calendar;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.calendar.models.Record;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder>
            implements Filterable {
    private ArrayList<Record> recordList;
    public ArrayList<Record> recordListForFilter;

    private HashMap<String, Integer> dayNamesColors;

    private RecordsListener listener;

    private Calendar calendar = Calendar.getInstance();

    private Filter recordsFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Record> recordListFiltered = new ArrayList<>();

            if (charSequence == null || charSequence.length() == 0) {
                recordListFiltered.addAll(recordListForFilter);
            } else {
                // trim deletes empty spaces
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for (Record record : recordListForFilter) {
                    if (record.getTitle().toLowerCase().contains(filterPattern)) {
                        recordListFiltered.add(record);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = recordListFiltered;

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            recordList.clear();
            recordList.addAll((ArrayList)filterResults.values);

            notifyDataSetChanged();
        }
    };

    private Comparator<Record> sortByDate = new Comparator<Record>() {
        @Override
        public int compare(Record record, Record t1) {
            if (record.getDate().getTime() < t1.getDate().getTime()) return 1;
            else if (record.getDate().getTime() > t1.getDate().getTime()) return -1;
            else return 0;
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;

        public ViewHolder(CardView v) {
            super(v);
            cardView = v;
        }
    }

    public RecordsAdapter(ArrayList<Record> recordList) {
        this.recordList = recordList;
        recordListForFilter = new ArrayList<>(this.recordList);
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

    @Override
    public Filter getFilter() {
        return recordsFilter;
    }

    public void addRecord(Record record) {
        recordList.add(record);
        recordListForFilter.add(record);

        Collections.sort(recordList, sortByDate);
        Collections.sort(recordListForFilter, sortByDate);

        notifyDataSetChanged();
    }

    public void addRecord(Record record, Calendar calendar) {
        recordList.add(record);
        recordListForFilter.add(record);

        filterByDate(calendar);
    }

    public void updateRecord(int position, Record record) {
        recordList.get(position).setTitle(record.getTitle());
        recordList.get(position).setText(record.getText());
        recordList.get(position).setDate(record.getDate());
        recordList.get(position).setPhotos(record.getPhotos());

        Record toDelete = recordList.get(position);
        recordList.remove(position);

        recordList.add(toDelete);

        recordListForFilter.get(position).setTitle(record.getTitle());
        recordListForFilter.get(position).setText(record.getText());
        recordListForFilter.get(position).setDate(record.getDate());
        recordListForFilter.get(position).setPhotos(record.getPhotos());
        recordListForFilter.remove(position);
        recordListForFilter.add(toDelete);

        Collections.sort(recordList, sortByDate);
        Collections.sort(recordListForFilter, sortByDate);

        notifyDataSetChanged();
    }

    public void updateRecord(int position, Record record, Calendar calendar) {
        recordList.get(position).setTitle(record.getTitle());
        recordList.get(position).setText(record.getText());
        recordList.get(position).setDate(record.getDate());
        recordList.get(position).setPhotos(record.getPhotos());

        Record toDelete = recordList.get(position);
        recordList.remove(position);

        recordList.add(toDelete);

        recordListForFilter.get(position).setTitle(record.getTitle());
        recordListForFilter.get(position).setText(record.getText());
        recordListForFilter.get(position).setDate(record.getDate());
        recordListForFilter.get(position).setPhotos(record.getPhotos());
        recordListForFilter.remove(position);
        recordListForFilter.add(toDelete);

        filterByDate(calendar);
    }

    public void deleteRecord(int position) {
        recordList.remove(position);
        recordListForFilter.remove(position);

        notifyDataSetChanged();
    }

    public void clearRecords() {
        recordList.clear();
        recordListForFilter.clear();

        notifyDataSetChanged();
    }

    public void filterByDate(Calendar calendar) {
        ArrayList<Record> recordListFiltered = new ArrayList<>();
        Calendar calendar1 = Calendar.getInstance();

        for (Record record : recordListForFilter) {
            calendar1.setTime(record.getDate());
            Log.v(MainActivity.TAG, calendar1.get(Calendar.YEAR) + " " +
                    calendar1.get(Calendar.MONTH) + " " +
                    calendar1.get(Calendar.DATE));
            if (calendar1.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            calendar1.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            calendar1.get(Calendar.DATE) == calendar.get(Calendar.DATE)) {
                recordListFiltered.add(record);
            }
        }

        recordList.clear();
        recordList.addAll(recordListFiltered);
        Collections.sort(recordList, sortByDate);
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
