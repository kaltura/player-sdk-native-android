package com.kaltura.changemediademo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;

;


public class SelectMediaFragment extends Fragment {

    private MediaIdPostman mediaIdPostmanImplementor;

    public interface MediaIdPostman{
         public void postMediaId(String mediaId);
    }

    @Override
    public  void onAttach(Activity context){
        super.onAttach(context);
        this.mediaIdPostmanImplementor = (MainActivity)context;
    }
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

            View selectMediaFragmentView = inflater.inflate(R.layout.select_media_fragment, container, false);

            Bundle bundle = getArguments();
            String mediaId = bundle.getString(MainActivity.MEDIA_ID_KEY);

            //TextView selectMediaFragmentTextView = (TextView)selectMediaFragmentView.findViewById(R.id.select_media_fragment_textview);
            //selectMediaFragmentTextView.setText(mediaId);
            //Toast.makeText(this.getContext(), "Orig Media Id = " + mediaId, Toast.LENGTH_SHORT).show();
            return selectMediaFragmentView;
        }

      @Override
      public void  onActivityCreated(Bundle savedInstanceState){
          super.onActivityCreated(savedInstanceState);
          Button changeMediaButton = (Button)getActivity().findViewById(R.id.select_media_button);
          changeMediaButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  Spinner mediaIsSpinner = (Spinner)getActivity().findViewById(R.id.media_id_spinner);
                  mediaIdPostmanImplementor.postMediaId(mediaIsSpinner.getSelectedItem().toString());
                  //getActivity().getFragmentManager().popBackStackImmediate();
              }
          });
      }

      public void displayResult(String result){
          //getActivity().findViewById(R.id.select_media_fragment_textview);
      }

}
