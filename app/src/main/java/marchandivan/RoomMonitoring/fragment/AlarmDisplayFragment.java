package marchandivan.RoomMonitoring.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.SweepGradient;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.db.AlarmConfig;
import marchandivan.RoomMonitoring.dialog.AlarmDialogBuilder;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlarmDisplayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmDisplayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlarmDisplayFragment extends Fragment {
    private static final String ARG_SENSOR = "sensor";
    private static final String ARG_ALARM_ID = "alarm_id";
    private static final String EVEN_ROW = "even_row";

    private long mSensor;
    private boolean mEvenRow;
    private AlarmConfig.Alarm mAlarm;
    private Fragment mFragment;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlarmDisplayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlarmDisplayFragment newInstance(long sensor, long alarmId, boolean evenRow) {
        AlarmDisplayFragment fragment = new AlarmDisplayFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SENSOR, sensor);
        args.putLong(ARG_ALARM_ID, alarmId);
        args.putBoolean(EVEN_ROW, evenRow);
        fragment.setArguments(args);
        return fragment;
    }

    public AlarmDisplayFragment() {
        // Required empty public constructor
    }

    private void updateDisplay(final View view) {
        // Display temp range
        TextView alarmMinTemp = (TextView)view.findViewById(R.id.alarm_min_temp);
        alarmMinTemp.setText(String.valueOf(mAlarm.mMinTemp));
        TextView alarmMaxTemp = (TextView)view.findViewById(R.id.alarm_max_temp);
        alarmMaxTemp.setText(String.valueOf(mAlarm.mMaxTemp));

        // Display alarm on/off icon
        final Switch alarmEnabled = (Switch)view.findViewById(R.id.alarm_enabled);
        alarmEnabled.setChecked(mAlarm.mAlarmActive);
        alarmEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update alarm config
                AlarmConfig alarmConfig = new AlarmConfig(buttonView.getContext(), mSensor);
                mAlarm.mAlarmActive = isChecked;
                alarmConfig.update(mAlarm);
                Toast.makeText(buttonView.getContext(),
                        buttonView.getContext().getString(R.string.alarm_is_onoff, buttonView.getContext().getString(isChecked ? R.string.on : R.string.off)),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Display time range
        if(!mAlarm.isActiveAnyTime())
        {
            // Make the layout visible
            LinearLayout alarmTimeDisplay = (LinearLayout)view.findViewById(R.id.alarm_time_display);
            alarmTimeDisplay.setVisibility(View.VISIBLE);

            // Start/Stop time
            TextView alarmStartTime = (TextView)view.findViewById(R.id.alarm_start_time);
            alarmStartTime.setText(String.format("%02d:%02d", mAlarm.mStartTime.first, mAlarm.mStartTime.second));
            TextView alarmStopTime = (TextView)view.findViewById(R.id.alarm_stop_time);
            alarmStopTime.setText(String.format("%02d:%02d", mAlarm.mStopTime.first, mAlarm.mStopTime.second));
        }

        // Setup onclick listeners
        ImageButton editAlarm = (ImageButton)view.findViewById(R.id.edit_alarm);
        editAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlarmDialogBuilder builder = new AlarmDialogBuilder(v.getContext());

                // Set post save callback to update display
                builder.setPostSaveCallback(new Runnable() {
                    @Override
                    public void run() {
                        // Update display
                        updateDisplay(view);
                    }
                });

                // Show the dialog
                AlertDialog dialog = builder.edit(mSensor, mAlarm);
                dialog.show();

            }
        });
        ImageButton deleteAlarm = (ImageButton)view.findViewById(R.id.delete_alarm);
        deleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Build the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Are you sure you want to remove this alarm?");
                builder.setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete alarm from DB
                        AlarmConfig alarmConfig = new AlarmConfig(mFragment.getActivity(), mSensor);
                        alarmConfig.delete(mAlarm);

                        // Remove fragment
                        FragmentManager fragmentManager = mFragment.getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.remove(mFragment);
                        fragmentTransaction.commit();

                    }
                });

                // Show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = this;
        if (getArguments() != null) {
            mSensor = getArguments().getLong(ARG_SENSOR);
            long alarmId = getArguments().getLong(ARG_ALARM_ID);
            mEvenRow = getArguments().getBoolean(EVEN_ROW);

            // Get alarm config
            AlarmConfig alarmConfig = new AlarmConfig(this.getActivity(), mSensor);
            mAlarm = alarmConfig.read(alarmId);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_alarm_display, container, false);
        if (!mEvenRow) {
            view.setBackgroundColor(getResources().getColor(R.color.light_gray));
        }

        // Update display
        if (mAlarm != null) {
            updateDisplay(view);
        }

        return view;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
