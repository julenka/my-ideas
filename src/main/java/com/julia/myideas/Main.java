package com.julia.myideas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.client.android.AsyncNoteStoreClient;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.thrift.transport.TTransportException;

import org.joda.time.Instant;
import org.joda.time.Interval;

import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class Main extends Activity {
	public static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.PRODUCTION;

	public static final String NOTE_TITLE = "intitle:'My Ideas'";
	public static final int MIN_MESSAGE_LENGTH = 10;
	

	// TODO: I may want to refactor this into a singleton/part of Application
	EvernoteSession evernoteSession;
	String authToken;
	AsyncNoteStoreClient noteStoreClient;

	// GUID for the 'my ideas' note. Used to update the note contents
	IdeasModel model;
	String ideasNoteGuid = "";
	
	AlertDialog.Builder myDialog;


	Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		myDialog = new AlertDialog.Builder(this).setNeutralButton("okay", null);
		
		setContentView(R.layout.activity_main);
		// Get your authentication key
		if(initEvernote()) {
			onLoggedIn();
		}
	}
	
	private void showPopupMessage(String message) {
		myDialog.setMessage(message).show();
	}

	/**
	 * Get the idea entered by the user. Does error checking and pops up dialog accordingly.
	 * Returns null if message is invalid
	 */
	private Idea getUserEnteredIdea() {
		EditText editText = (EditText)findViewById(R.id.editText1);
		final String ideaStr = editText.getText().toString().trim();

		if(ideaStr.length() == 0) {
			showPopupMessage("Gotta enter something!");
			return null;
		} else if(ideaStr.length() < MIN_MESSAGE_LENGTH) {
			showPopupMessage("This idea is a bit short, can you flesh it out a bit?");
			return null;
		} else if (model == null || ideasNoteGuid == null || ideasNoteGuid.length() == 0) {
			showPopupMessage("ERROR: Ideas note not found!");
			return null;
		}
		return new Idea(ideaStr);
	}

	private void hideKeyboard() {
		EditText myEditText = (EditText) findViewById(R.id.editText1);  
		InputMethodManager imm = (InputMethodManager)getSystemService(
		      Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(myEditText.getWindowToken(), 0);
	}
	
	public void addIdea(View v) {
		Idea idea = getUserEnteredIdea();
		if(idea == null) return;
		
		hideKeyboard();
		
		toast("Adding your idea...");
		model.addIdea(idea, new Runnable() {
			@Override
			public void run() {
				toast("Successfully added your idea!");
				readyForAnIdea(true);
			}
		}, new Runnable() {
			@Override
			public void run() {
				toast("Error adding idea, check logcat");
			}
		});
	}

	private void toast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(Main.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	private void onLoggedOut(){
		hideView(R.id.buttonSubmit);
		hideView(R.id.editText1);
		hideView(R.id.linearLayout1);
		hideView(R.id.textNumIdeasSoFar);
		showView(R.id.buttonLogin);
	}

	private void hideView(int id) {
		(findViewById(id)).setVisibility(View.GONE);
	}
	private void showView(int id) {
		(findViewById(id)).setVisibility(View.VISIBLE);
	}
	public void login(View v) {
		evernoteSession.authenticate(this);
	}

	private void readyForAnIdea(boolean anotherOne) {
		String message = "Ready for an idea! Enter your idea here";
		if(anotherOne) {
			message = "Got it! Enter another idea here";
		}
		textHint(message);
		(findViewById(R.id.editText1)).requestFocus();
	}
	
	private void textHint(String hint) {
		EditText et = ((EditText)findViewById(R.id.editText1));
		et.setText("");
		et.setHint(hint);
	}
	
	/**
	 * Initialize everything associated with evernote.
	 * Login, get auth token, and get the guid of the my ideas token
	 * @return true if initialization suceeded, else false
	 */
	private boolean initEvernote() {
		evernoteSession = EvernoteSession.getInstance(this, 
				getResources().getString(R.string.consumer_key), 
				getResources().getString(R.string.consumer_secret), 
				EVERNOTE_SERVICE);

		if(!evernoteSession.isLoggedIn()) {
			toast("You need to log in first!");
			evernoteSession.authenticate(this);
			return false;
		}

		NoteFilter miscIdeasFilter = new NoteFilter();
		miscIdeasFilter.setWords(NOTE_TITLE);
		miscIdeasFilter.setOrder(NoteSortOrder.TITLE.ordinal());
		
		authToken = evernoteSession.getAuthToken();
		try {
			noteStoreClient = evernoteSession.getClientFactory().createNoteStoreClient();
		} catch (TTransportException e) {
			textHint("Error connecting to evernote service. Do you have internet? If so, try logging in again.");
			onLoggedOut();
			return false;
		}
		
		// get the GUID for the my ideas note
		noteStoreClient.findNotes(miscIdeasFilter, 0, 1, new OnClientCallback<NoteList>() {
			@Override
			public void onSuccess(NoteList data) {
				
				if(data.getNotes().size() < 1) {
					textHint("Error finding 'My Ideas' note. Is the note there?");
				} else {
					Note n = data.getNotes().get(0);
					ideasNoteGuid = n.getGuid();
					try {
						model = new IdeasModel(noteStoreClient, ideasNoteGuid, handler);
					} catch (ParserConfigurationException e) {
						e.printStackTrace();
					}
					
					model.addIdeaRefreshCallback(new Runnable(){
						@Override
						public void run() {
							LinearLayout l1 = (LinearLayout)findViewById(R.id.linearLayout1);
							
							l1.removeAllViews();
							List<Idea> ideas = model.getIdeas();
							Collections.sort(ideas);
							for(Idea i : ideas){
								TextView t1 = new TextView(Main.this);
								t1.setText(i.toString());
								l1.addView(t1);
								HorizontalDivider d1 = new HorizontalDivider(Main.this);
								l1.addView(d1);
							
							}
							Interval interval = new Interval(new Instant(ideas.get(ideas.size() - 1).created), 
									new Instant());
							
							long nDays = interval.toDuration().getStandardDays();
							((TextView)(findViewById(R.id.textNumIdeasSoFar))).setText(ideas.size() + " ideas so far in " + nDays + " days...");
						}
					});
					model.refreshIdeas();
					handler.post(new Runnable(){
						public void run() {
							readyForAnIdea(false);
						}
					});
				}
			}

			@Override
			public void onException(Exception exception) {
				textHint("Couldn't communicate with Evernote :-( Do you have internet?");
			}
		});
							
		// Enable the idea submit button once we'e initialized the note store
		return true;
		
	}
	
	private void onLoggedIn() {
		showView(R.id.buttonSubmit);
		showView(R.id.editText1);
		hideView(R.id.buttonLogin);
		showView(R.id.linearLayout1);
		showView(R.id.textNumIdeasSoFar);
		findViewById(R.id.buttonSubmit).setEnabled(true);
		textHint("Successfully initialized evernote! Finding my ideas note...");
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		//Update UI when oauth activity returns result
		case EvernoteSession.REQUEST_CODE_OAUTH:
			if (resultCode == Activity.RESULT_OK) {
				if(initEvernote()) {
					onLoggedIn();
				}
			} else {
				onLoggedOut();
			}
			break;
		}
	}
}
