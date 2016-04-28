package com.cs296.kainrath.cs296project;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisplayInterests extends AppCompatActivity {

    private User user;
    private ArrayList<String> interests;
    private ListView list;
    private InterestAdaptor list_adapter;
    private ArrayList<Integer> selected_indices = new ArrayList<Integer>();
    private Button button_delete;
    private boolean modified = false;
    private static String TAG = "DisplayInts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_display_load);

        if (savedInstanceState != null) {
            ((GlobalVars) this.getApplication()).restoreState(savedInstanceState);
        }

        if (GlobalVars.getFailed()) {
            System.exit(1);
        }
        user = GlobalVars.getUser();
        if (user == null) {
            startActivity(new Intent(this, CreateUser.class));
        }
        setContentView(R.layout.activity_display_interests);
        displayInterests();
        registerOnClick();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        user.setInterests(interests);
        ((GlobalVars) this.getApplication()).saveState(savedInstanceState);
        if (modified) {
            List<String> origInterests = user.getInterests();
            List<String> newInterests;
            List<String> deletedInterests;
            if (origInterests == null) {
                deletedInterests = new ArrayList<>();
                newInterests = interests;
            } else {
                newInterests = new ArrayList<>(interests);
                deletedInterests = new ArrayList<>(origInterests);
                newInterests.removeAll(origInterests);
                deletedInterests.removeAll(interests);
            }
            user.setInterests(interests);
            new AsyncUpdateUser(newInterests, deletedInterests).execute(user.getId());
        }
    }

    private void displayInterests() {
        button_delete = (Button) findViewById(R.id.button_remove);
        button_delete.setEnabled(false);
        list = (ListView) findViewById(R.id.list_interests);
        if (GlobalVars.getUser().getInterests() == null) {
            GlobalVars.getUser().setInterests(new ArrayList<String>());
        }
        interests = new ArrayList<>(GlobalVars.getUser().getInterests());

        list_adapter = new InterestAdaptor(this, R.layout.list_item, interests, selected_indices);
        list.setAdapter(list_adapter);
    }

    private void registerOnClick() {
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view_clicked, int position, long id) {
                ImageView icon = (ImageView) view_clicked.findViewById(R.id.list_image);
                if (selected_indices.contains(position)) {
                    selected_indices.remove(Integer.valueOf(position));
                    icon.setImageResource(android.R.drawable.checkbox_off_background);
                    if (selected_indices.isEmpty()) {
                        button_delete.setEnabled(false);
                    }
                } else {
                    selected_indices.add(position);
                    icon.setImageResource(android.R.drawable.checkbox_on_background);
                    button_delete.setEnabled(true);
                }
            }
        });
    }

    public void onClickAdd(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add an Interest");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!interests.contains(input.getText().toString().trim())) {
                    interests.add(input.getText().toString().trim());
                    modified = true;
                    list_adapter.notifyDataSetChanged();
                    Log.d(TAG, "added an interest");
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void onClickRemove(View view) {
        Collections.sort(selected_indices, Collections.reverseOrder());
        for (int index : selected_indices) {
            interests.remove(index);
        }
        selected_indices.clear();
        if (interests.isEmpty()) {
            button_delete.setEnabled(false);
        }
        modified = true;
        list_adapter.notifyDataSetChanged();
        Log.d(TAG, "removed interest(s)");
    }

    public void onClickReturn(View view) {
        Log.d(TAG, "Returning to main activity");
        if (modified) {
            modified = false;
            Log.d(TAG, "Interests have been modified, need to update DB");
            List<String> origInterests = user.getInterests();
            List<String> newInterests;
            List<String> deletedInterests;
            if (origInterests == null) {
                for (int i = 0; i < interests.size(); ++i) {
                    Log.d(TAG, "new interest: " + interests.get(i));
                }
                newInterests = interests;
                deletedInterests = new ArrayList<>();
                deletedInterests.add("");
                Log.d(TAG, "no interests to delete");
            } else {
                newInterests = new ArrayList<>(interests);
                Log.d(TAG, "newInterests size: " + newInterests.size());
                deletedInterests = new ArrayList<>(origInterests);
                newInterests.removeAll(origInterests);
                Log.d(TAG, "newInterests size: " + newInterests.size());
                deletedInterests.removeAll(interests);
                if (newInterests.isEmpty()) {
                    newInterests.add("");
                    Log.d(TAG, "No new interests");
                }
                if (deletedInterests.isEmpty()) {
                    deletedInterests.add("");
                    Log.d(TAG, "No interests to remove");
                }
                for (String ints : newInterests) {
                    Log.d(TAG, "new interest: " + ints);
                }
                for (String ints : deletedInterests) {
                    Log.d(TAG, "deleted interest: " + ints);
                }
            }
            user.setInterests(interests);
            new AsyncUpdateUser(newInterests, deletedInterests).execute(user.getId());
        } else {
            Log.d(TAG, "Interests have not been modified");
        }
        startActivity(new Intent(this, MainActivity.class));
    }
}

class InterestAdaptor extends ArrayAdapter<String> {

    List<String> interest_list;
    List<Integer> selected;

    private static LayoutInflater inflater=null;

    public InterestAdaptor(Context context, int textResId, ArrayList<String> list, ArrayList<Integer> selected) {
        super(context, textResId, list);
        this.interest_list = list;
        this.selected = selected;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return interest_list.size();
    }

    @Override
    public String getItem(int position) {
        return interest_list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.list_item, null);
        }

        TextView item =(TextView) rowView.findViewById(R.id.list_single_item);
        item.setText(interest_list.get(position));
        ImageView icon = (ImageView) rowView.findViewById(R.id.list_image);
        if (selected.contains(position)) {
            icon.setImageResource(android.R.drawable.checkbox_on_background);
        } else {
            icon.setImageResource(android.R.drawable.checkbox_off_background);
        }
        return rowView;
    }

}