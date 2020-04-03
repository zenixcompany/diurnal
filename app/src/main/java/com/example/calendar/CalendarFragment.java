package com.example.calendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import com.example.calendar.models.Record;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class CalendarFragment extends AppCompatActivity implements DatePickerDialog.OnDateSetListener{

    private ArrayList<Record> recordList;
    private HashMap<String, Integer> dayNamesColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Button button = (Button)findViewById(R.id.calendarActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment datePicker = new DatePickerFragment();
                datePicker.show(getSupportFragmentManager(), "date picker");
            }
        });
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        String currentDateString = DateFormat.getDateInstance().format(calendar.getTime());

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(currentDateString);
    }

    public CalendarFragment(ArrayList<Record> recordList) {
        this.recordList = recordList;
        initColors();
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

    /*
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
     */
}
