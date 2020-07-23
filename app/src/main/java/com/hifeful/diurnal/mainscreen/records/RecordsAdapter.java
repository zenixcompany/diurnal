package com.hifeful.diurnal.mainscreen.records;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hifeful.diurnal.R;
import com.hifeful.diurnal.data.Record;
import com.hifeful.diurnal.mainscreen.MainScreenActivity;

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
    private Context context;

    public ArrayList<Record> recordList;
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

    private Comparator<Record> sortByDate = (record, t1) -> {
        if (record.getDate().getTime() < t1.getDate().getTime()) return 1;
        else if (record.getDate().getTime() > t1.getDate().getTime()) return -1;
        else return 0;
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;

        public ViewHolder(FrameLayout v) {
            super(v);
            cardView = v.findViewById(R.id.card_record);
        }
    }

    public RecordsAdapter(Context context, ArrayList<Record> recordList) {
        this.context = context;
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
        FrameLayout view = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_record,
                parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Locale currentLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = context.getResources().getConfiguration().locale;
        }

        CardView cardView = holder.cardView;

        TextView dayName = cardView.findViewById(R.id.date_day_text);
        TextView dayNumber = cardView.findViewById(R.id.date_day_number);
        TextView month = cardView.findViewById(R.id.date_month);

        calendar.setTime(recordList.get(position).getDate());
        String dayNameStrDefault = Objects.requireNonNull(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
        String dayNameStr = Objects.requireNonNull(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, currentLocale));
        dayName.setText(dayNameStr.toUpperCase());

        dayName.setBackgroundColor(dayNamesColors.get(dayNameStrDefault));

        dayNumber.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        month.setText(Objects.requireNonNull(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, currentLocale)).toUpperCase());

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
        Record toDelete = recordList.remove(position);
        recordList.add(record);

        recordListForFilter.remove(toDelete);
        recordListForFilter.add(record);

        Collections.sort(recordList, sortByDate);
        Collections.sort(recordListForFilter, sortByDate);

        notifyDataSetChanged();
    }

    public void updateRecord(int position, Record record, Calendar calendar) {
        Record toDelete = recordList.remove(position);
        recordList.add(record);

        recordListForFilter.remove(toDelete);
        recordListForFilter.add(record);

        filterByDate(calendar);
    }

    public void deleteRecord(int position) {
        Record toDelete = recordList.remove(position);
        recordListForFilter.remove(toDelete);

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
        dayNamesColors.put("Mon", context.getResources().getColor(R.color.redMaterial, context.getTheme()));
        dayNamesColors.put("Tue", context.getResources().getColor(R.color.pinkMaterial, context.getTheme()));
        dayNamesColors.put("Wed", context.getResources().getColor(R.color.purpleMaterial, context.getTheme()));
        dayNamesColors.put("Thu", context.getResources().getColor(R.color.blueMaterial, context.getTheme()));
        dayNamesColors.put("Fri", context.getResources().getColor(R.color.greenMaterial, context.getTheme()));
        dayNamesColors.put("Sat", context.getResources().getColor(R.color.orangeMaterial, context.getTheme()));
        dayNamesColors.put("Sun", context.getResources().getColor(R.color.yellowMaterial, context.getTheme()));
    }
}
