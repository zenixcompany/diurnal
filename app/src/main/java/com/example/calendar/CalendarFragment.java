package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.calendar.models.Record;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class CalendarFragment extends Fragment {
    public CalendarView calendarView;

    private FirebaseFirestore db;
    private CollectionReference collectionReference;
    public RecordsAdapter recordsAdapter;

    private ArrayList<EventDay> events;

    public CalendarFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.records_calendar);
        events = new ArrayList<>();

        RecyclerView calendarRecycler = view.findViewById(R.id.calendar_recycler);

        ArrayList<Record> recordList = new ArrayList<>();
        recordsAdapter = new RecordsAdapter(recordList);
        recordsAdapter.setListener(position -> {
            Intent intent = new Intent(getActivity(), RecordActivity.class);

            intent.putExtra(RecordActivity.ACTION, RecordActivity.EDIT_NOTE);
            intent.putExtra(RecordActivity.NOTE_ID, recordList.get(position).getNote_id());
            intent.putExtra(RecordActivity.NOTE_POSITION, position);
            intent.putExtra(RecordActivity.TITLE, recordList.get(position).getTitle());
            intent.putExtra(RecordActivity.RECORD, recordList.get(position).getText());
            intent.putExtra(RecordActivity.PHOTOS, recordList.get(position).getPhotos());

            getActivity().startActivityForResult(intent, MainActivity.EDIT_NOTE);
        });

        db = FirebaseFirestore.getInstance();
        collectionReference = db.collection("records");
        getNotes();

        calendarRecycler.setAdapter(recordsAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        calendarRecycler.setLayoutManager(layoutManager);

        // arrows
        calendarView.setPreviousButtonImage(ResourcesCompat.getDrawable(getResources(),
                R.drawable.ic_keyboard_arrow_left_black_24dp, null));
        calendarView.setForwardButtonImage(ResourcesCompat.getDrawable(getResources(),
                R.drawable.ic_keyboard_arrow_right, null));

        calendarView.setEvents(events);

        calendarView.setOnDayClickListener(eventDay -> {
            Calendar calendar = eventDay.getCalendar();
            recordsAdapter.filterByDate(calendar);
        });

        return view;
    }

    public void updateCalendarDots() {
        events.clear();
        events = new ArrayList<>();
        for(Record record : recordsAdapter.recordListForFilter) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(record.getDate());

            Calendar calendar1 = Calendar.getInstance();
            calendar1.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
            calendar1.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
            calendar1.set(Calendar.DATE, calendar.get(Calendar.DATE));
            events.add(new EventDay(calendar1, R.drawable.ic_brightness_1_black_24dp));
        }
        calendarView.setEvents(events);
    }

    public void getNotes() {
        db = FirebaseFirestore.getInstance();

        collectionReference = db.collection("records");

        Query recordsQuery;

        recordsQuery = collectionReference.whereEqualTo("user_id",
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .orderBy("date", Query.Direction.ASCENDING);

        recordsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.v(MainActivity.TAG, "Shit is being displayed");

                for (QueryDocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                    Record record = documentSnapshot.toObject(Record.class);
                    recordsAdapter.addRecord(record);

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(record.getDate());

                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                    calendar1.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                    calendar1.set(Calendar.DATE, calendar.get(Calendar.DATE));
                    events.add(new EventDay(calendar1, R.drawable.ic_brightness_1_black_24dp));
                }
                calendarView.setEvents(events);
                recordsAdapter.filterByDate(calendarView.getFirstSelectedDate());
            } else {
                Log.v(MainActivity.TAG, "Some shitty problem happened");
            }
        });
    }
}
