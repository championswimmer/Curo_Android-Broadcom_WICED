package in.ac.dtu.curo.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Arrays;

import in.ac.dtu.curo.CuroMainActivity;
import in.ac.dtu.curo.R;
import in.ac.dtu.curo.customer.BillingActivity;
import in.ac.dtu.curo.customer.FindProductActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CustomerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomerFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CustomerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CustomerFragment newInstance(String param1, String param2) {
        CustomerFragment fragment = new CustomerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public CustomerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        byte[] gyroData = new byte[19];
        ((CuroMainActivity) getActivity()).processSensorData(gyroData, new CuroMainActivity.OnValueReturned() {
            @Override
            public void returnValue(String instrument, int[] values) {
                if (instrument.equals("ACCEL")) {
                    Log.d("CURO", "ACCEL" + Arrays.toString(values));
                }
                if (instrument.equals("GYRO")) {
                    Log.d("CURO", "GYRO" + Arrays.toString(values));
                }
                if (instrument.equals("MAGNET")) {
                    Log.d("CURO", "MAGNET" + Arrays.toString(values));
                }
            }
        });
        Log.d("CURO", Arrays.toString(gyroData));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_for_ustomer, container, false);

        ((Button) rootView.findViewById(R.id.btn_billing)).setOnClickListener(this);
        ((Button) rootView.findViewById(R.id.btn_find_product)).setOnClickListener(this);

        return rootView;
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_billing) {
            Intent i = new Intent(getActivity(), BillingActivity.class);
            startActivity(i);
        }

        if (view.getId() == R.id.btn_find_product) {
            Intent i = new Intent(getActivity(), FindProductActivity.class);
            startActivity(i);

        }
    }
}
