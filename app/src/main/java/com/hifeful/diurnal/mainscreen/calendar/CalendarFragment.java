package com.hifeful.diurnal.mainscreen.calendar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hifeful.diurnal.R;
import com.hifeful.diurnal.mainscreen.MainScreenActivity;
import com.hifeful.diurnal.mainscreen.records.RecordsAdapter;
import com.hifeful.diurnal.record.RecordActivity;
import com.hifeful.diurnal.data.Record;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class CalendarFragment extends Fragment {
    // UI
    private RecyclerView calendarRecycler;
    private TextView emptyListTextView;
    public MaterialCalendarView calendarView;
    private DotsDecorator dotsDecorator;
    private TodayDecorator todayDecorator;

    // Variables
    private ArrayList<Record> recordList;
    public RecordsAdapter recordsAdapter;
    private ArrayList<CalendarDay> calendarDays;

    private FirebaseFirestore db;
    private CollectionReference collectionReference;

    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance() {
        Bundle args = new Bundle();
        CalendarFragment fragment = new CalendarFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        view.post(() -> {
            float density = getResources().getDisplayMetrics().density;
            float dpWidth = view.getWidth() / density;
            if (dpWidth < 600) {
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(view.getWidth(),
                        view.getHeight() / 2);
                calendarView.setLayoutParams(layoutParams);

            } else {
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 600,
                        getResources().getDisplayMetrics());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width,
                        view.getHeight() / 2);
                layoutParams.gravity = Gravity.CENTER;
                calendarView.setLayoutParams(layoutParams);

            }
            calendarView.setSelectionColor(getResources().getColor(R.color.colorOnSurface,
                    getContext().getTheme()));
        });

        emptyListTextView = view.findViewById(R.id.calendar_day_empty);
        calendarView = view.findViewById(R.id.records_calendar);

        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_OTHER_MONTHS);
        calendarView.setTileSize(RelativeLayout.LayoutParams.MATCH_PARENT);

        calendarDays = new ArrayList<>();

        DaysDecorator daysDecorator = new DaysDecorator(getContext(), calendarView);
        calendarView.addDecorator(daysDecorator);

        calendarView.setOnMonthChangedListener((widget, date) -> {
            Log.i(MainScreenActivity.TAG, "onMonthChanged: ");
            calendarView.removeDecorator(todayDecorator);
            calendarView.removeDecorator(daysDecorator);
            calendarView.invalidateDecorators();
            calendarView.addDecorator(daysDecorator);
            calendarView.addDecorator(todayDecorator);
        });

        todayDecorator = new TodayDecorator(getContext());
        calendarView.addDecorator(todayDecorator);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            recordsAdapter.filterByDate(date.getCalendar());
            setUpRecyclerVisibility();
        });
        if (savedInstanceState != null) {
            CalendarDay selectedDate = savedInstanceState.getParcelable("selectedDate");
            calendarView.setDateSelected(selectedDate, true);
        } else {
            calendarView.setDateSelected(Calendar.getInstance(), true);
        }

        calendarRecycler = view.findViewById(R.id.calendar_recycler);

        recordList = new ArrayList<>();
        recordsAdapter = new RecordsAdapter(getContext(), recordList);
        recordsAdapter.setListener(position -> {
            Intent intent = new Intent(getActivity(), RecordActivity.class);

            intent.putExtra(RecordActivity.ACTION, RecordActivity.EDIT_NOTE);
            intent.putExtra(RecordActivity.NOTE_ID, recordList.get(position).getNote_id());
            intent.putExtra(RecordActivity.NOTE_POSITION, position);
            intent.putExtra(RecordActivity.TITLE, recordList.get(position).getTitle());
            intent.putExtra(RecordActivity.RECORD, recordList.get(position).getText());
            intent.putExtra(RecordActivity.DATE, recordList.get(position).getDate());
            intent.putExtra(RecordActivity.PHOTOS, recordList.get(position).getPhotos());

            getActivity().startActivityForResult(intent, MainScreenActivity.EDIT_NOTE);
        });

        db = FirebaseFirestore.getInstance();
        collectionReference = db.collection("records");
        getRecordsFromDatabase();

        calendarRecycler.setAdapter(recordsAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        calendarRecycler.setLayoutManager(layoutManager);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("selectedDate", calendarView.getSelectedDate());
    }

    public void updateCalendarDots() {
        if (dotsDecorator != null) {
            calendarDays.clear();
            calendarView.removeDecorator(dotsDecorator);
            calendarView.invalidateDecorators();
        }
        for(Record record : recordsAdapter.recordListForFilter) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(record.getDate());

            calendarDays.add(CalendarDay.from(calendar));
        }
        dotsDecorator = new DotsDecorator(Color.RED, calendarDays);
        calendarView.addDecorator(dotsDecorator);

        setUpRecyclerVisibility();
    }

    public void getRecordsFromDatabase() {
        db = FirebaseFirestore.getInstance();

        collectionReference = db.collection("records");

        Query recordsQuery;

        recordsQuery = collectionReference.whereEqualTo("user_id",
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .orderBy("date", Query.Direction.ASCENDING);

        recordsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.v(MainScreenActivity.TAG, "Shit is being displayed");
                for (QueryDocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                    Record record = documentSnapshot.toObject(Record.class);
                    Collections.reverse(record.getPhotos());
                    recordsAdapter.addRecord(record);

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(record.getDate());
                }
                Log.i(MainScreenActivity.TAG, calendarView.getSelectedDate().toString());
                recordsAdapter.filterByDate(calendarView.getSelectedDate().getCalendar());
                updateCalendarDots();
            } else {
                Log.v(MainScreenActivity.TAG, "Some shitty problem happened");
            }
        });
    }

    private void setUpRecyclerVisibility() {
        if (recordList.isEmpty()) {
            calendarRecycler.setVisibility(View.GONE);
            emptyListTextView.setVisibility(View.VISIBLE);
        } else {
            calendarRecycler.setVisibility(View.VISIBLE);
            emptyListTextView.setVisibility(View.GONE);
        }
    }
}
