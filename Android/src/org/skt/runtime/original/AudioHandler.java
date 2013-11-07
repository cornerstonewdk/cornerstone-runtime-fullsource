/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.skt.runtime.original;

import android.content.Context;
import android.media.AudioManager;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.skt.runtime.api.LOG;
import org.skt.runtime.api.Plugin;
import org.skt.runtime.api.PluginResult;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class called by SKTRuntimeActivity to play and record audio.  
 * The file can be local or over a network using http.
 * 
 * Audio formats supported (tested):
 * 	.mp3, .wav
 * 
 * Local audio files must reside in one of two places:
 * 		android_asset: 		file name must start with /android_asset/sound.mp3
 * 		sdcard:				file name is just sound.mp3
 */
public class AudioHandler extends Plugin {

    public static String TAG = "AudioHandler";
	HashMap<String,AudioPlayer> players;	// Audio player object
	ArrayList<AudioPlayer> pausedForPhone;     // Audio players that were paused when phone call came in
	
	/**
	 * Constructor.
	 */
	public AudioHandler() {
		this.players = new HashMap<String,AudioPlayer>();
		this.pausedForPhone = new ArrayList<AudioPlayer>();
	}

	/**
	 * Executes the request and returns PluginResult.
	 * 
	 * @param action 		The action to execute.
	 * @param args 			JSONArry of arguments for the plugin.
	 * @param callbackId	The callback id used when calling back into JavaScript.
	 * @return 				A PluginResult object with a status and message.
	 */
	public PluginResult execute(String action, JSONArray args, String callbackId) {
		PluginResult.Status status = PluginResult.Status.OK;
		String result = "";		
		
		try {
			if (action.equals("startRecordingAudio")) {
				this.startRecordingAudio(args.getString(0), args.getString(1));
			}
			else if (action.equals("stopRecordingAudio")) {
				this.stopRecordingAudio(args.getString(0));
			}
			else if (action.equals("startPlayingAudio")) {
				this.startPlayingAudio(args.getString(0), args.getString(1));
			}
			else if (action.equals("seekToAudio")) {
				this.seekToAudio(args.getString(0), args.getInt(1));
			}
			else if (action.equals("pausePlayingAudio")) {
				this.pausePlayingAudio(args.getString(0));
			}
			else if (action.equals("stopPlayingAudio")) {
				this.stopPlayingAudio(args.getString(0));
			} else if (action.equals("setVolume")) {
			   try {
				   this.setVolume(args.getString(0), Float.parseFloat(args.getString(1)));
			   } catch (NumberFormatException nfe) {
				   //no-op
			   }
			} else if (action.equals("getCurrentPositionAudio")) {
				float f = this.getCurrentPositionAudio(args.getString(0));
				return new PluginResult(status, f);
			}
			else if (action.equals("getDurationAudio")) {
				float f = this.getDurationAudio(args.getString(0), args.getString(1));
				return new PluginResult(status, f);
			}
			else if (action.equals("release")) {
				boolean b = this.release(args.getString(0));
				return new PluginResult(status, b);
			}
			return new PluginResult(status, result);
		} catch (JSONException e) {
			e.printStackTrace();
			return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
		}
	}

	/**
	 * Identifies if action to be executed returns a value and should be run synchronously.
	 * 
	 * @param action	The action to execute
	 * @return			T=returns value
	 */
	public boolean isSynch(String action) {
		if (action.equals("getCurrentPositionAudio")) {
			return true;
		}
		else if (action.equals("getDurationAudio")) {
			return true;
		}
		return false;
	}

	/**
	 * Stop all audio players and recorders.
	 */
	public void onDestroy() {
        for (AudioPlayer audio : this.players.values()) {
            audio.destroy();
        }
        this.players.clear();
	}
	
    /**
     * Called when a message is sent to plugin. 
     * 
     * @param id            The message id
     * @param data          The message data
     */
    public void onMessage(String id, Object data) {
        
        // If phone message
        if (id.equals("telephone")) {
            
            // If phone ringing, then pause playing
            if ("ringing".equals(data) || "offhook".equals(data)) {
                
                // Get all audio players and pause them
                for (AudioPlayer audio : this.players.values()) {
                    if (audio.getState() == AudioPlayer.MEDIA_RUNNING) {
                        this.pausedForPhone.add(audio);
                        audio.pausePlaying();
                    }
                }

            }
            
            // If phone idle, then resume playing those players we paused
            else if ("idle".equals(data)) {
                for (AudioPlayer audio : this.pausedForPhone) {
                    audio.startPlaying(null);
                }
                this.pausedForPhone.clear();
            }
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------
	
	/**
	 * Release the audio player instance to save memory.
	 * 
	 * @param id				The id of the audio player
	 */
	private boolean release(String id) {
    	if (!this.players.containsKey(id)) {
    		return false;
    	}
    	AudioPlayer audio = this.players.get(id);
    	this.players.remove(id);
    	audio.destroy();
    	return true;
	}

	/**
	 * Start recording and save the specified file.
	 * 
	 * @param id				The id of the audio player
	 * @param file				The name of the file
	 */
    public void startRecordingAudio(String id, String file) {
    	// If already recording, then just return;
    	if (this.players.containsKey(id)) {
    		return;
    	}
    	AudioPlayer audio = new AudioPlayer(this, id);
    	this.players.put(id, audio);
    	audio.startRecording(file);
    }

    /**
     * Stop recording and save to the file specified when recording started.
     * 
	 * @param id				The id of the audio player
     */
    public void stopRecordingAudio(String id) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		audio.stopRecording();
    		this.players.remove(id);
    	}
    }
    
    /**
     * Start or resume playing audio file.
     * 
	 * @param id				The id of the audio player
     * @param file				The name of the audio file.
     */
    public void startPlayingAudio(String id, String file) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio == null) {
    		audio = new AudioPlayer(this, id);
    		this.players.put(id, audio);
    	}
    	audio.startPlaying(file);
    }

    /**
     * Seek to a location.
     * 
     * 
	 * @param id				The id of the audio player
	 * @param miliseconds		int: number of milliseconds to skip 1000 = 1 second
     */
    public void seekToAudio(String id, int milliseconds) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		audio.seekToPlaying(milliseconds);
    	}
    }
    
    /**
     * Pause playing.
     * 
	 * @param id				The id of the audio player
     */
    public void pausePlayingAudio(String id) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		audio.pausePlaying();
    	}
    }

    /**
     * Stop playing the audio file.
     * 
	 * @param id				The id of the audio player
     */
    public void stopPlayingAudio(String id) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		audio.stopPlaying();
    		//audio.destroy();
    		//this.players.remove(id);
    	}
    }
    
    /**
     * Get current position of playback.
     * 
	 * @param id				The id of the audio player
     * @return 					position in msec
     */
    public float getCurrentPositionAudio(String id) {
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		return(audio.getCurrentPosition()/1000.0f);
    	}
    	return -1;
    }
    
    /**
     * Get the duration of the audio file.
     * 
	 * @param id				The id of the audio player
     * @param file				The name of the audio file.
     * @return					The duration in msec.
     */
    public float getDurationAudio(String id, String file) {
    	
    	// Get audio file
    	AudioPlayer audio = this.players.get(id);
    	if (audio != null) {
    		return(audio.getDuration(file));
    	}
    	
    	// If not already open, then open the file
    	else {
    		audio = new AudioPlayer(this, id);
    		this.players.put(id, audio);
    		return(audio.getDuration(file));
    	}
    }  
    
    /**
     * Set the audio device to be used for playback.
     * 
     * @param output			1=earpiece, 2=speaker
     */
    public void setAudioOutputDevice(int output) {
		AudioManager audiMgr = (AudioManager) this.ctx.getSystemService(Context.AUDIO_SERVICE);
		if (output == 2) {
			audiMgr.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_SPEAKER, AudioManager.ROUTE_ALL);
		}
		else if (output == 1) {
			audiMgr.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
		}
		else {
			System.out.println("AudioHandler.setAudioOutputDevice() Error: Unknown output device.");
		}
    }
    
    /**
     * Get the audio device to be used for playback.
     * 
     * @return					1=earpiece, 2=speaker
     */
    public int getAudioOutputDevice() {
		AudioManager audiMgr = (AudioManager) this.ctx.getSystemService(Context.AUDIO_SERVICE);
		if (audiMgr.getRouting(AudioManager.MODE_NORMAL) == AudioManager.ROUTE_EARPIECE) {
			return 1;
		}
		else if (audiMgr.getRouting(AudioManager.MODE_NORMAL) == AudioManager.ROUTE_SPEAKER) {
			return 2;
		}
		else {
			return -1;
		}
    }

    /**
     * Set the volume for an audio device
     *
     * @param id				The id of the audio player
     * @param volume            Volume to adjust to 0.0f - 1.0f
     */
    public void setVolume(String id, float volume) {
        AudioPlayer audio = this.players.get(id);
        if (audio != null) {
            audio.setVolume(volume);
        } else {
            System.out.println("AudioHandler.setVolume() Error: Unknown Audio Player " + id);
        }
    }
}
