package com.aware.plugin.mobile_sensor.activities;

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.aware.plugin.mobile_sensor.Plugin;
import com.aware.plugin.mobile_sensor.R;
import com.infteh.comboseekbar.ComboSeekBar;
import com.infteh.comboseekbar.ComboSeekBar.Dot;

public class StressActivity extends Activity {
	
	static StressActivity activity = null; 
	private ComboSeekBar mSeekBar; //segmented seekbar for likert scale (0-7)
	private ComboSeekBar mSeekBarLoud;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		setContentView(R.layout.activity_stress);
		final LayoutInflater li = LayoutInflater.from(StressActivity.this);
	    View content = li.inflate(R.layout.stress_rating, null);
	    final List<String> seekBarStep = Arrays.asList("0","1","2","3","4","5","6","7");
		mSeekBar = (ComboSeekBar) content.findViewById(R.id.seekbar);
		mSeekBar.setAdapter(seekBarStep);
		final Dialog mainDialog = new Dialog(mSeekBar.getContext(),android.R.style.Theme_DeviceDefault_Light_Dialog);
		mainDialog.setContentView(content);
		mainDialog.setTitle("Stress Rating");
		Button b = (Button) content.findViewById(R.id.level_ok);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Dot selected = ComboSeekBar.dotSelected();
				Plugin.rating  = selected.text;
				Intent intent = getIntent();
				if(intent.getExtras().getBoolean("Noise")){
//					Log.d("Stress","inside onClick()");
					View loudContent = li.inflate(R.layout.loudness_rating, null);
					mSeekBarLoud = (ComboSeekBar) loudContent.findViewById(R.id.seekbar);
					mSeekBarLoud.setAdapter(seekBarStep);
					final Dialog dialog = new Dialog(mSeekBarLoud.getContext(),android.R.style.Theme_DeviceDefault_Light_Dialog);
					dialog.setContentView(loudContent);
					dialog.setTitle("Loudness Rating");
					Button b = (Button) loudContent.findViewById(R.id.level_ok);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							Dot selected = ComboSeekBar.dotSelected();
							Plugin.loudnessRating = selected.text;
//							Log.d("Loud",Plugin.loudnessRating);
							dialog.dismiss();
							mainDialog.dismiss();
							finish();
						}
					});
					dialog.setCancelable(false);
					dialog.show();
				}else{
					finish();
				}
			}
		});
		mainDialog.setCancelable(false);
	    mainDialog.show();
	}

	public static StressActivity getInstance() {
		return activity;
	}
}
