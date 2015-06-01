package com.julia.myideas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import android.os.Handler;
import android.util.Log;

import com.evernote.client.android.AsyncNoteStoreClient;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.Note;

public class IdeasModel {
	private String noteGuid;
	private AsyncNoteStoreClient noteStoreClient;
	Handler handler;
	List<Idea> ideas;
	List<Runnable> ideaRefreshCallbacks = new ArrayList<Runnable>();
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
	
	public void addIdeaRefreshCallback(Runnable r) {
		ideaRefreshCallbacks.add(r);
	}
	
	public List<Idea> getIdeas() {
		return ideas;
	}
	
	public IdeasModel(
			AsyncNoteStoreClient noteStoreClient,
			String noteGuid,
			Handler handler) throws ParserConfigurationException {
		this.noteGuid = noteGuid;
		this.noteStoreClient = noteStoreClient;
		this.handler = handler;
		dBuilder = dbFactory.newDocumentBuilder();
	}
	
	public void addIdea(final Idea idea, final Runnable onSuccess, final Runnable onFail){
		// Find 'ideas' note via getNote
		noteStoreClient.getNote(noteGuid, true, false, false, false, new OnClientCallback<Note>() {
			@Override
			public void onSuccess(Note data) {
				String content = data.getContent();
				Log.d(getClass().getName(), content);
				int insertionPoint = findNoteInsertionPoint(content);
				content = content.substring(0, insertionPoint) + "<div>[[ " + idea + " ]]</div>" + content.substring(insertionPoint);
				data.setContent(content);
				noteStoreClient.updateNote(data, new OnClientCallback<Note>() {
					
					@Override
					public void onSuccess(Note data) {
						handler.post(onSuccess);
						refreshIdeas();
					}
					
					@Override
					public void onException(Exception exception) {
						handler.post(onFail);
						Log.d(getClass().getName(), "error saving idea: " + exception.toString());
					}
				});
			}
			
			@Override
			public void onException(Exception exception) {
				Log.d(getClass().getName(), "error getting idea content: " + exception.toString());
				handler.post(onFail);
			}
		});
	}
	Pattern p = Pattern.compile("\\[\\[(.*?)\\]\\]");
	public void refreshIdeas() {
		noteStoreClient.getNote(noteGuid, true, false, false, false, new OnClientCallback<Note>() {
			@Override
			public void onSuccess(Note data) {
				String content = data.getContent();
				Matcher m = p.matcher(content);
				ideas = new ArrayList<Idea>();

				while(m.find()) {
					String[] strs = m.group(1).split(":");
					if(strs.length != 2) {
						continue;
					}
					Idea toadd = new Idea(strs[0], strs[1]);
					ideas.add(toadd);
					Log.d(getClass().getName(), "adding: " + toadd.toString() );
				}
				for(Runnable r: ideaRefreshCallbacks) {
					handler.post(r);
				}	
			}
			
			@Override
			public void onException(Exception exception) {
				Log.e(IdeasModel.class.getName(), "error refreshing idea: " + exception.toString());
			}
		});
	}
	
	/**
	 * Returns insertion point where new idea should be added.
	 * Assumes taht we have 3 xml nodes before: <xml...><doctype...>and<en-note...>
	 * Findt the index of the third '>'
	 * Simplistic but it works!
	 * @param content
	 * @return the index where new note content should be added
	 */
	private int findNoteInsertionPoint(String content) {
		int closeBracketCount = 0;
		int i = 0;
		while(closeBracketCount < 3) {
			i++;
			if(content.charAt(i) == '>') closeBracketCount++;
		}
		return i + 1;
	}
}
