package com.example.calendar;

import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordsFragment extends Fragment {

    public RecordsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_records, container, false);

        RecyclerView recordsRecycler = view.findViewById(R.id.records_recycler);

        ArrayList<Record> records = new ArrayList<>();

        records.add(new Record(Calendar.getInstance().getTime()));
        records.add(new Record(Calendar.getInstance().getTime()));
        records.add(new Record(Calendar.getInstance().getTime()));

        RecordsAdapter recordsAdapter = new RecordsAdapter(records);
        recordsRecycler.setAdapter(recordsAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recordsRecycler.setLayoutManager(layoutManager);

        return view;
    }
}
