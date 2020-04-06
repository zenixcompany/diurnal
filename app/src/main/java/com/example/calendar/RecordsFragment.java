package com.example.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.calendar.models.Record;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Objects;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecordsFragment extends Fragment {
    public RecyclerView recordsRecycler;

    private ArrayList<Record> recordsList;
    public RecordsAdapter recordsAdapter;

    private FirebaseFirestore db;
    private CollectionReference collectionReference;
    private DocumentSnapshot lastQueriedDocument;

    public RecordsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_records, container, false);

        recordsRecycler = view.findViewById(R.id.records_recycler);

        recordsList = new ArrayList<>();
        recordsAdapter = new RecordsAdapter(recordsList);
        recordsAdapter.setListener(position -> {
            Intent intent = new Intent(getActivity(), RecordActivity.class);

            intent.putExtra(RecordActivity.ACTION, RecordActivity.EDIT_NOTE);
            intent.putExtra(RecordActivity.NOTE_ID, recordsList.get(position).getNote_id());
            intent.putExtra(RecordActivity.NOTE_POSITION, position);
            intent.putExtra(RecordActivity.TITLE, recordsList.get(position).getTitle());
            intent.putExtra(RecordActivity.RECORD, recordsList.get(position).getText());
            intent.putExtra(RecordActivity.DATE, recordsList.get(position).getDate());
            intent.putExtra(RecordActivity.PHOTOS, recordsList.get(position).getPhotos());

            getActivity().startActivityForResult(intent, MainActivity.EDIT_NOTE);
        });
        recordsRecycler.setAdapter(recordsAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recordsRecycler.setLayoutManager(layoutManager);

        getNotes();
        return view;
    }

    public void getNotes() {
        db = FirebaseFirestore.getInstance();

        collectionReference = db.collection("records");

        Query recordsQuery;
        if (lastQueriedDocument != null) {
            recordsQuery = collectionReference.whereEqualTo("user_id",
                    Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .orderBy("date", Query.Direction.ASCENDING)
                    .startAfter(lastQueriedDocument);
        } else {
            recordsQuery = collectionReference.whereEqualTo("user_id",
                    Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .orderBy("date", Query.Direction.ASCENDING);
        }

        recordsQuery.get().addOnCompleteListener(task -> {
           if (task.isSuccessful()) {
               Log.v(MainActivity.TAG, "Shit is being displayed");

               for (QueryDocumentSnapshot documentSnapshot : Objects.requireNonNull(task.getResult())) {
                   Record record = documentSnapshot.toObject(Record.class);
                   recordsAdapter.addRecord(record);
               }

               if (task.getResult().size() != 0) {
                   lastQueriedDocument = task.getResult().getDocuments()
                                        .get(task.getResult().size() - 1);
               }

                recordsAdapter.notifyDataSetChanged();
           } else {
               Log.v(MainActivity.TAG, "Some shitty problem happened");
           }
        });
    }
}
