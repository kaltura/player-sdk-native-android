package Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.kaltura.kalturaplayertoolkit.R;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LoginFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //retain value
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_login, container, false);
        Intent intent = getActivity().getIntent();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // check if this intent is started via browser
        if (Intent.ACTION_VIEW.equals( intent.getAction())) {
            Uri uri = intent.getData();
            String[] params = null;
            try {
                params = URLDecoder.decode(uri.toString(), "UTF-8").split(":=");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (params !=null && params.length > 1) {
                String iframeUrl = params[1];
                intent.putExtra(getString(R.string.prop_iframe_url), iframeUrl);
                FullscreenFragment newFragment = new FullscreenFragment();
                loadFragment(false, newFragment);
            } else {
                Log.w(TAG, "didn't load iframe, invalid iframeUrl parameter was passed");
            }

        }


        TextView infoMsg = (TextView)fragmentView.findViewById(R.id.powered);
        Spanned spanned = Html.fromHtml(getString(R.string.footer));
        infoMsg.setMovementMethod(LinkMovementMethod.getInstance());
        infoMsg.setText(spanned);

        Button demoBtn = (Button)fragmentView.findViewById(R.id.demoBtn);
        demoBtn.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PlayerFragment newFragment = new PlayerFragment();
                loadFragment(true, newFragment);
            }
        });
        return fragmentView;
    }

    private void loadFragment(boolean addToBackStack , Fragment newFragment){
        // Create fragment and give it an argument specifying the article it should show

        Bundle args = new Bundle();
        newFragment.setArguments(args);

// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);

        if(addToBackStack) {
            transaction.addToBackStack(null);
        }

// Commit the transaction
        transaction.commit();
    }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
