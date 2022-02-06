package com.dima.qwert;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ExtendedMail {

	Context mainContext;

	int SELECTION = 3;
	
	String title;
	String text;
	String from;
	String where;
	String attach;
	

    public void ExtendedMail(Context context) {
		mainContext=context;

        attach = "";

		sender_mail_async async_sending = new sender_mail_async();
		async_sending.execute();
    }

    
    private class sender_mail_async extends AsyncTask<String, String, Boolean> {
    	ProgressDialog WaitingDialog;

		@Override
		protected void onPreExecute() {
			WaitingDialog = ProgressDialog.show(mainContext, "..", "...", true);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			WaitingDialog.dismiss();
			Toast.makeText(mainContext, "...!!!", Toast.LENGTH_LONG).show();
			((Activity)mainContext).finish();
		}

		@Override
		protected Boolean doInBackground(String... params) {

			try {
				title = "";
				text = "";
				attach = "";
				if( params.length > 0 ){
					attach = params[0];
				}
				
				from = "dimanys111@gmail.com";
				where = "dimanys111@mail.ru";
				
                MailSenderClass sender = new MailSenderClass("dimanys111@gmail.com", "d84962907");
                
                sender.sendMail(title, text, from, where, attach);
			} catch (Exception e) {
				Toast.makeText(mainContext, "...!", Toast.LENGTH_SHORT).show();
			}
			
			return false;
		}
	}
}