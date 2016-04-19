package kaltura.com.kalturachangemediademo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by gilad.nadav on 29/03/2016.
 */
public class SelectMediaDialogFragment extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            // We build the dialog
            // getActivity() returns the Activity this Fragment is associated with
            AlertDialog.Builder theDialog = new AlertDialog.Builder(getActivity());

            // Set the title for the Dialog
            theDialog.setTitle("Sample Dialog");

            // Set the message
            theDialog.setMessage("Hello I'm a Dialog");

            // Add text for a positive button
            theDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Toast.makeText(getActivity(), "Clicked OK", Toast.LENGTH_SHORT).show();

                }
            });

            // Add text for a negative button
            theDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    Toast.makeText(getActivity(), "Clicked Cancel", Toast.LENGTH_SHORT).show();

                }
            });

            // Returns the created dialog
            return theDialog.create();
        }
}

