package com.kaltura.testapp;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private boolean isResumed = false;
    private View mFragmentView = null;
    private PlayerViewController mPlayerView;
    private KPPlayerConfig mConfig;

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

    public void setPlayerConfig(KPPlayerConfig config) {
        mConfig = config;
    }

    public void pausePlayer() {
        mPlayerView.releaseAndSavePosition();
    }

    public void killPlayer() {
        mPlayerView.removePlayer();
        mPlayerView = null;
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
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
//                    Log.d("customplgin", eventName);
//                }
//            });
            KPPlayerConfig config = null;
            if (mConfig == null) {
                config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.44.rc5/mwEmbedFrame.php", "32855491", "1424501");
                config.setEntryId("1_32865911");
//            config.addConfig("proxyData", "{\"MediaID\":\"296461\",\"iMediaID\":\"296461\",\"initObj\":{\"ApiPass\":\"11111\",\"ApiUser\":\"tvpapi_225\",\"DomainID\":\"282672\",\"Locale\":{\"LocaleCountry\":\"\",\"LocaleDevice\":\"\",\"LocaleLanguage\":\"\",\"LocaleUserState\":\"Unknown\"},\"Platform\":\"Cellular\",\"SiteGuid\":\"6142289\",\"UDID\":\"123456\"},\"mediaType\":\"0\",\"picSize\":\"640x360\",\"withDynamic\":\"false\"}");
//            config.addConfig("tvpapiGetLicensedLinks.plugin", "true");
//            config.addConfig("TVPAPIBaseUrl", "http://tvpapi-stg.as.tvinci.com/v3_4/gateways/jsonpostgw.aspx?m");
//            config.addConfig("liveCore.disableLiveCheck", "true");
//            KPPlayerConfig config = new  KPPlayerConfig("http://cdnapi.kaltura.com", "31638861", "1831271");
//            config.setEntryId("1_ng282arr");
//            config.addConfig("doubleClick.plugin", "true");
//            config.addConfig("doubleClick.adTagUrl", "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=xml_vmap1&unviewed_position_start=1&cust_params=sample_ar%3Dpremidpost&cmsid=496&vid=short_onecue&correlator=[timestamp]");

            } else {
                config = mConfig;
                mPlayerView.setCustomSourceURLProvider((OfflineActivity)getActivity());
            }
            mPlayerView.initWithConfiguration(config);
            mPlayerView.addKPlayerEventListener("play", "play1", new PlayerViewController.EventListener() {
                @Override
                public void handler(String eventName, String params) {

                }
            });
            mPlayerView.addEventListener(new KPEventListener() {
                @Override
                public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                    Log.d("KPlayer State Changed", state.toString());
                }

                @Override
                public void onKPlayerError(PlayerViewController playerViewController, KPError error) {

                }

                @Override
                public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
                    Log.d("KPlayer State Changed", Float.toString(currentTime));
                }

                @Override
                public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
                    Log.d("KPlayer toggeled", Boolean.toString(isFullscreen));
                }
            });
//            mPlayerView.setPlayerViewControllerAdapter(new PlayerViewController.PlayerViewControllerAdapter() {
//                @Override
//                public String localURLForEntryId(String entryId) {
//                    String videoPath = "android.resource://" + getActivity().getPackageName() + "/" + R.raw.demovideo;
//                    return videoPath;
//                }
//            });
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
