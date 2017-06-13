package com.disarm.sanna.pdm;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.disarm.sanna.pdm.Adapters.MyAdapter;
import com.disarm.sanna.pdm.Util.DividerItemDecoration;
import com.disarm.sanna.pdm.Util.Reset;
import com.disarm.sanna.pdm.location.LocationState;


import java.io.File;


public class SurakshitActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    static String root = Environment.getExternalStorageDirectory().toString();
    public final static String TARGET_DMS_PATH = root + "/DMS/";
    public static int[] prgmNameList = {R.string.health,
            R.string.food,
            R.string.shelter,
            R.string.victim};


    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_surakshit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(prgmNameList);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Intent intent = new Intent(getApplicationContext(), ActivityList.class);
                if (position == 0) {
                    intent.putExtra("IntentType", "Health");
                } else if (position == 1) {
                    intent.putExtra("IntentType", "Food");
                } else if (position == 2) {
                    intent.putExtra("IntentType", "Shelter");
                } else if (position == 3) {
                    intent.putExtra("IntentType", "Victim");
                }
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.webView) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.43.1:8000"));
            startActivity(browserIntent);
        } else if (id == R.id.mapView) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8080/getMapAsset/index.html"));
            startActivity(browserIntent);
        } else if (id == R.id.reset) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage(R.string.reset_working)
                    .setCancelable(false)
                    .setPositiveButton(R.string.reset,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    File dir = new File(Environment.getExternalStorageDirectory() + "/DMS/Working");
                                    if (Reset.deleteContents(dir)) {
                                        Toast.makeText(SurakshitActivity.this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
            alertDialogBuilder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else if (id == R.id.resetall) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage(R.string.reset_all_data)
                    .setCancelable(false)
                    .setPositiveButton(R.string.reset,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    ProgressDialog pd = new ProgressDialog(SurakshitActivity.this);
                                    pd.show();
                                    pd.setMessage("Reset is on its way !");
                                    File dir = new File(Environment.getExternalStorageDirectory() + "/DMS");
                                    if (Reset.deleteContents(dir)) {
                                        pd.setMessage("Reset Done");
                                        Toast.makeText(SurakshitActivity.this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                                    }
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SurakshitActivity.this);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.clear();
                                    editor.commit();
                                    pd.setMessage("Restarting");
                                    Intent i = getBaseContext().getPackageManager()
                                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    finish();
                                    startActivity(i);
                                    pd.dismiss();
                                }
                            });
            alertDialogBuilder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else if (id == R.id.exit) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }
}


