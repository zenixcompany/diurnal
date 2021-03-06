package com.hifeful.diurnal.mainscreen.records;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hifeful.diurnal.R;
import com.hifeful.diurnal.mainscreen.MainScreenActivity;
import com.hifeful.diurnal.record.RecordActivity;
import com.hifeful.diurnal.data.Record;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.hifeful.diurnal.mainscreen.MainScreenActivity.TAG;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordsFragment extends Fragment {
    // UI
    public RecyclerView recordsRecycler;
    private LinearLayoutManager mLayoutManager;
    private TextView emptyListTextView;

    // Variables
    private ArrayList<Record> recordsList;
    public RecordsAdapter recordsAdapter;
    private Parcelable mListState;

    private FirebaseFirestore db;
    private CollectionReference collectionReference;

    public RecordsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_records, container, false);

        recordsRecycler = view.findViewById(R.id.records_recycler);
        emptyListTextView = view.findViewById(R.id.records_empty);

        recordsList = new ArrayList<>();
        recordsAdapter = new RecordsAdapter(getContext(), recordsList);

        recordsAdapter.setListener(position -> {
            Intent intent = new Intent(getActivity(), RecordActivity.class);

            intent.putExtra(RecordActivity.ACTION, RecordActivity.EDIT_NOTE);
            intent.putExtra(RecordActivity.NOTE_ID, recordsList.get(position).getNote_id());
            intent.putExtra(RecordActivity.NOTE_POSITION, position);
            intent.putExtra(RecordActivity.TITLE, recordsList.get(position).getTitle());
            intent.putExtra(RecordActivity.RECORD, recordsList.get(position).getText());
            intent.putExtra(RecordActivity.DATE, recordsList.get(position).getDate());
            intent.putExtra(RecordActivity.PHOTOS, recordsList.get(position).getPhotos());

            getActivity().startActivityForResult(intent, MainScreenActivity.EDIT_NOTE);
        });
        recordsRecycler.setAdapter(recordsAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        recordsRecycler.setLayoutManager(mLayoutManager);

        if (savedInstanceState != null) {

            ArrayList<Record> restoredItems = savedInstanceState.getParcelableArrayList("items");
            ArrayList<Record> restoredItemsForFilter = savedInstanceState.getParcelableArrayList("itemsForFilter");
            if (restoredItems != null) {
                recordsList.addAll(restoredItems);
            }
            if (restoredItemsForFilter != null) {
                recordsAdapter.recordListForFilter.addAll(restoredItemsForFilter);
            }
            recordsAdapter.notifyDataSetChanged();
            mListState = savedInstanceState.getParcelable("state");
        } else {
            getRecordsFromDatabase();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mListState != null) {
            mLayoutManager.onRestoreInstanceState(mListState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("items", recordsList);
        outState.putParcelableArrayList("itemsForFilter", recordsAdapter.recordListForFilter);
        outState.putParcelable("state", mLayoutManager.onSaveInstanceState());
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
               Log.v(TAG, "Shit is being displayed");
               for (QueryDocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                   Record record = documentSnapshot.toObject(Record.class);
                   Collections.reverse(record.getPhotos());
                   recordsAdapter.addRecord(record);
               }
               if (getActivity() != null) {
                   String query = ((MainScreenActivity)getActivity()).getSearchViewQuery();
                   recordsAdapter.getFilter().filter(query);
                   setUpRecyclerVisibility();
               }
           } else {
               Log.v(TAG, "Some shitty problem happened");
           }
        });
    }

    public void setUpRecyclerVisibility() {
        emptyListTextView.setVisibility(recordsList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
