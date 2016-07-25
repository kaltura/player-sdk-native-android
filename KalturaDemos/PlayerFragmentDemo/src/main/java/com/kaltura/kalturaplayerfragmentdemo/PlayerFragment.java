package com.kaltura.kalturaplayerfragmentdemo;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment implements KPEventListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = "PlayerFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private boolean isResumed = false;
    private View mFragmentView = null;
    private PlayerViewController mPlayerView;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlayerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlayerFragment newInstance(String param1, String param2) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public PlayerFragment() {
        // Required empty public constructor
    }

    public void pausePlayer() {
        mPlayerView.releaseAndSavePosition();
    }

    public void killPlayer() {
        if (mPlayerView != null) {
            mPlayerView.removePlayer();
            mPlayerView = null;
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
        }
    }


    public void resumePlayer() {
        mPlayerView.resumePlayer();
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
        if(mFragmentView == null) {
            mFragmentView = inflater.inflate(R.layout.fragment_fullscreen, container, false);
            mPlayerView = (PlayerViewController) mFragmentView.findViewById(R.id.player);
            mPlayerView.loadPlayerIntoActivity(getActivity());

//            mPlayerView.addKPlayerEventListener("onEnableKeyboardBinding", "someID", new PlayerViewController.EventListener() {
//                @Override
//                public void handler(String eventName, String params) {
//                    LOGD("customplgin", eventName);
//                }
//            });
            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/branches/master/mwEmbedFrame.php", "20540612", "243342").setEntryId("1_sf5ovm7u");
            mPlayerView.initWithConfiguration(config);


            
            // Add swipe rcogniser
            final GestureDetector gestureDetector = new GestureDetector(getActivity(), new CustomeGestureDetector());
            mPlayerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            });




            mPlayerView.addEventListener(new KPEventListener() {
                @Override
                public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                    LOGD(TAG, "KPlayer State Changed " + state.toString());
                }

                @Override
                public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
                    LOGD(TAG, "KPlayer onKPlayerPlayheadUpdate " + Float.toString(currentTime));
                }

                @Override
                public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
                    LOGD(TAG, "KPlayer onKPlayerFullScreenToggeled " +  Boolean.toString(isFullscreen));
                }

                @Override
                public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
                    LOGE(TAG, "KPlayer onKPlayerError " + error.getErrorMsg());
                }
            });
        }


        return mFragmentView;
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
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        LOGD(TAG, "KPlayer State Changed " + state.toString());
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {

    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        LOGD(TAG, "KPlayer onKPlayerPlayheadUpdate " +Float.toString(currentTime));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

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


    // Gesture recogniser
    private class CustomeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(e1 == null || e2 == null) return false;
            if(e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            else {
                try { // right to left swipe .. go to next page
                    if(e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 800) {
                        //do your stuff
                        return true;
                    } //left to right swipe .. go to prev page
                    else if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 800) {
                        //do your stuff
                        return true;
                    } //bottom to top, go to next document
                    else if(e1.getY() - e2.getY() > 100 && Math.abs(velocityY) > 800) {
                        //do your stuff
                        return true;
                    } //top to bottom, go to prev document
                    else if (e2.getY() - e1.getY() > 100 && Math.abs(velocityY) > 800 ) {
                        //do your stuff
                        return true;
                    }
                } catch (Exception e) { // nothing
                }
                return false;
            }
        }
    }

}
