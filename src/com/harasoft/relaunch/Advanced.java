package com.harasoft.relaunch;

import android.app.Activity;
import android.content.*;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Advanced extends Activity {
	final static String TAG = "Advanced";

	SharedPreferences prefs;
	ReLaunchApp app;
	Button rebootBtn;
	Button powerOffBtn;
	String[] ingnoreFs;
	List<Info> infos;
	int myId = -1;
	boolean addSView = true;

	WifiManager wfm;
	public static boolean wifiOn = false;
	Button wifiOnOff;
	Button wifiScan;
	List<NetInfo> wifiNetworks = new ArrayList<NetInfo>();
	WiFiAdapter adapter;
	ListView lv_wifi;
	IntentFilter i1;
	IntentFilter i2;
	BroadcastReceiver b1;
	BroadcastReceiver b2;
	String connectedSSID;

	static class NetInfo {
		static int unknownLevel = -5000;
		String SSID;
		String extra;
		int level;
		int netId;
		boolean inrange;
		boolean configured;

		NetInfo(String s, int id, boolean in, boolean conf) {
			SSID = s;
			extra = "";
			level = unknownLevel;
			netId = id;
			inrange = in;
			configured = conf;
		}

		NetInfo(String s, boolean in, boolean conf) {
			SSID = s;
			extra = "";
			level = unknownLevel;
			netId = 0;
			inrange = in;
			configured = conf;
		}

		NetInfo(String s, String e, int id, boolean in, boolean conf) {
			SSID = s;
			extra = e;
			level = unknownLevel;
			netId = id;
			inrange = in;
			configured = conf;
		}

		NetInfo(String s, String e, boolean in, boolean conf) {
			SSID = s;
			extra = e;
			level = unknownLevel;
			netId = 0;
			inrange = in;
			configured = conf;
		}
	}

	public class NetInfoComparator implements java.util.Comparator<NetInfo> {
		public int compare(NetInfo o1, NetInfo o2) {
			if (connectedSSID != null && connectedSSID.equals(o1.SSID)) {
				return -1;
			}
			if (connectedSSID != null && connectedSSID.equals(o2.SSID)) {
				return 1;
			}
			if (o1.inrange && !o2.inrange)
				return -1;
			if (!o1.inrange && o2.inrange)
				return 1;
			if (o1.level < o2.level)
				return 1;
			if (o1.level > o2.level)
				return -1;
			return o1.SSID.compareToIgnoreCase(o2.SSID);
		}
	}

	static class Info {
		String dev;
		String mpoint;
		String fs;
		String total;
		String used;
		String free;
		boolean ro;
	}

	static class ViewHolder {
		TextView tv1;
		TextView tv2;
		TextView tv3;
		ImageView iv;
	}

	class WiFiAdapter extends BaseAdapter {
		final Context cntx;

		WiFiAdapter(Context context) {
			cntx = context;
		}

		public int getCount() {
			return wifiNetworks.size();
		}

		public Object getItem(int position) {
			return wifiNetworks.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView,
				ViewGroup parent) {
			ViewHolder holder;
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getApplicationContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.adv_wifi_item, null);
				holder = new ViewHolder();
				holder.tv1 = (TextView) v.findViewById(R.id.wf_ssid);
				holder.tv2 = (TextView) v.findViewById(R.id.wf_capabilities);
				holder.tv3 = (TextView) v.findViewById(R.id.wf_other);
				holder.iv = (ImageView) v.findViewById(R.id.wf_icon);
				v.setTag(holder);
			} else
				holder = (ViewHolder) v.getTag();
			TextView tv1 = holder.tv1;
			TextView tv2 = holder.tv2;
			TextView tv3 = holder.tv3;
			ImageView iv = holder.iv;
			final WifiInfo winfo = wfm.getConnectionInfo();
			final NetInfo item = wifiNetworks.get(position);
			if (item != null) {
				if (item.inrange && item.configured) {
					tv1.setBackgroundColor(getResources().getColor(
							R.color.file_reading_bg));
					tv1.setTextColor(getResources().getColor(
							R.color.file_reading_fg));
					tv2.setBackgroundColor(getResources().getColor(
							R.color.file_reading_bg));
					tv2.setTextColor(getResources().getColor(
							R.color.file_reading_fg));
					tv3.setBackgroundColor(getResources().getColor(
							R.color.file_reading_bg));
					tv3.setTextColor(getResources().getColor(
							R.color.file_reading_fg));
					iv.setImageDrawable(getResources().getDrawable(
							R.drawable.file_ok));
				} else {
					tv1.setBackgroundColor(getResources().getColor(
							R.color.file_finished_bg));
					tv1.setTextColor(getResources().getColor(
							R.color.file_finished_fg));
					tv2.setBackgroundColor(getResources().getColor(
							R.color.file_finished_bg));
					tv2.setTextColor(getResources().getColor(
							R.color.file_finished_fg));
					tv3.setBackgroundColor(getResources().getColor(
							R.color.file_finished_bg));
					tv3.setTextColor(getResources().getColor(
							R.color.file_finished_fg));
					iv.setImageDrawable(getResources().getDrawable(
							R.drawable.file_notok));
				}

				if (item.SSID.equals(winfo.getSSID())) {
					SpannableString s1 = new SpannableString(item.SSID);
					s1.setSpan(Typeface.BOLD, 0, item.SSID.length(), 0);
					tv1.setText(s1);
					if (item.extra.equals(""))
						tv2.setText("");
					else {
						SpannableString s2 = new SpannableString(item.extra);
						s2.setSpan(Typeface.BOLD, 0, item.extra.length(), 0);
						tv2.setText(s2);
					}
					int ipAddress = winfo.getIpAddress();
					// "Connected, IP: %d.%d.%d.%d"
					String s = String.format(
							getResources().getString(
									R.string.jv_advanced_connected)
									+ " %d.%d.%d.%d", (ipAddress & 0xff),
							(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
							(ipAddress >> 24 & 0xff));
					int sl1 = s.length();
					// ", Level: "
					s += ", "
							+ getResources().getString(
									R.string.jv_advanced_level) + " "
							+ item.level + "dBm " + levelToString(item.level);
					SpannableString s3 = new SpannableString(s);
					s3.setSpan(Typeface.BOLD, 0, sl1, 0);
					tv3.setText(s3);
				} else {
					SpannableString s1 = new SpannableString(item.SSID);
					s1.setSpan(Typeface.BOLD, 0, item.SSID.length(), 0);
					tv1.setText(s1);
					tv2.setText(item.extra);
					String s;
					if (item.inrange)
						// "Level: "
						s = getResources()
								.getString(R.string.jv_advanced_level)
								+ " "
								+ item.level
								+ "dBm "
								+ levelToString(item.level);
					else
						// "Not in range"
						s = getResources().getString(
								R.string.jv_advanced_notrange);
					if (!item.configured)
						// ", not configured"
						s += ", "
								+ getResources().getString(
										R.string.jv_advanced_not_configured);
					tv3.setText(s);
				}
			}
			return v;
		}
	}

	private String levelToString(int level) {
		if (level >= -56)
			return "[\u25A0\u25A0\u25A0\u25A0\u25A0]";
		if (level >= -63)
			return "[\u25A0\u25A0\u25A0\u25A0\u25A1]";
		if (level >= -70)
			return "[\u25A0\u25A0\u25A0\u25A1\u25A1]";
		if (level >= -77)
			return "[\u25A0\u25A0\u25A1\u25A1\u25A1]";
		if (level >= -84)
			return "[\u25A0\u25A1\u25A1\u25A1\u25A1]";
		return "[\u25A1\u25A1\u25A1\u25A1\u25A1]";
	}

	private String sp(int n) {
		String rc = "";
		for (int i = 0; i < n; i++)
			rc += "&nbsp;";
		return rc;
	}

	private int getMyId() {
		int rc = -1;

		for (String l : execFg("id")) {
			Pattern p = Pattern.compile("uid=(\\d+)\\(");
			Matcher m = p.matcher(l);
			if (m.find()) {
				try {
					rc = Integer.parseInt(m.group(1));
				} catch (NumberFormatException e) {
					rc = -1;
				}
				if (rc != -1)
					return rc;
			}
		}
		return rc;
	}

	// Read file and return result as list of strings
	private List<String> readFile(String fname) {
		BufferedReader br;
		String readLine;
		List<String> rc = new ArrayList<String>();
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					fname)), 1000);
		} catch (FileNotFoundException e) {
			return rc;
		}
		try {
			while ((readLine = br.readLine()) != null)
				rc.add(readLine);
		} catch (IOException e) {
			try {
				br.close();
			} catch (IOException e1) {
			}
			return rc;
		}
		try {
			br.close();
		} catch (IOException e) {
		}

		return rc;
	}

	// Execute foreground command and return result as list of strings
	private List<String> execFg(String cmd) {
		List<String> rc = new ArrayList<String>();
		try {

			Process p = Runtime.getRuntime().exec(cmd);
			try {
				DataInputStream ds = new DataInputStream(p.getInputStream());
				String line;
				while (true) {
					line = ds.readLine();
					if (line == null)
						break;
					rc.add(line);
				}
			} finally {
				p.destroy();
			}
		} catch (IOException e) {
		}
		return rc;
	}

	private void createInfo() {
		// Filesystem
		infos = new ArrayList<Info>();
		for (String s : readFile("/proc/mounts")) {
			String[] f = s.split("\\s+");
			if (f.length < 4)
				continue;
			String fs = f[2];
			String flags = f[3];
			String[] f1 = flags.split(",");
			boolean ignore = false;
			for (int i = 0; i < ingnoreFs.length; i++){
				if (ingnoreFs[i].equals(fs)) {
					ignore = true;
					break;
				}
            }
			if (ignore)
				continue;
			Info in = new Info();
			in.dev = f[0];
			in.mpoint = f[1];
			in.fs = fs;
			for (int i = 0; i < f1.length; i++){
				if (f1[i].equals("ro")) {
					in.ro = true;
					break;
				} else if (f1[i].equals("rw")) {
					in.ro = false;
					break;
				}
            }
			in.total = "0";
			in.used = "0";
			in.free = "0";
			for (String l : execFg("df " + in.mpoint)) {
				String[] e = l.split("\\s+");
				if (e.length < 6)
					continue;
				in.total = e[1];
				in.used = e[3];
				in.free = e[5];
			}

			infos.add(in);
		}

		// UID
		myId = getMyId();

		// Wifi
		wfm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiOn = wfm.isWifiEnabled();
        wfm.getWifiState();
		wifiNetworks = readScanResults(wfm);
	}

	private List<NetInfo> readScanResults(WifiManager w) {
		List<NetInfo> rc = new ArrayList<NetInfo>();
		List<ScanResult> rc1 = w.getScanResults();
		List<WifiConfiguration> rc2 = w.getConfiguredNetworks();

		connectedSSID = w.getConnectionInfo().getSSID();

		if (rc1 == null) {
			// No scan results - just copy configured networks to returned value
			for (WifiConfiguration wc : rc2)
				rc.add(new NetInfo(wc.SSID, wc.networkId, false, true));
			Collections.sort(rc, new NetInfoComparator());
			return rc;
		}

		// Merge uniq scanresult items with configured network info
		for (ScanResult s : rc1) {
			boolean alreadyHere = false;
			for (NetInfo s1 : rc)
				if (s1.SSID.equals(s.SSID)) {
					alreadyHere = true;
					s1.level = s.level;
					break;
				}
			if (!alreadyHere) {
				boolean in = false;
				for (WifiConfiguration wc : rc2) {
					String ssid = wc.SSID;
					if (ssid.startsWith("\"") && ssid.endsWith("\""))
						ssid = ssid.substring(1, ssid.length() - 1);
					if (ssid.equals(s.SSID)) {
						rc.add(new NetInfo(ssid, s.capabilities, wc.networkId,
								true, true));
						rc.get(rc.size() - 1).level = s.level;
						in = true;
						break;
					}
				}
				if (!in)
					// In range but not configured
					rc.add(new NetInfo(s.SSID, s.capabilities, true, false));
				rc.get(rc.size() - 1).level = s.level;
			}
		}

		// Add confiured but not active networks
		for (WifiConfiguration wc : rc2) {
			String ssid = wc.SSID;
			if (ssid.startsWith("\"") && ssid.endsWith("\""))
				ssid = ssid.substring(1, ssid.length() - 1);
			boolean alreadyHere = false;
			for (NetInfo s : rc)
				if (s.SSID.equals(ssid)) {
					alreadyHere = true;
					break;
				}
			if (!alreadyHere)
				rc.add(new NetInfo(ssid, false, true));
		}
		Collections.sort(rc, new NetInfoComparator());
		return rc;
	}

	private void updateWiFiInfo() {
		wifiOnOff.setEnabled(true);
		if (wifiOn) {
			// "Turn WiFi off"
			wifiOnOff.setText(getResources().getString(
					R.string.jv_advanced_turn_wifi_off));
			wifiOnOff.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.wifi_off), getResources()
					.getDrawable(R.drawable.ci_wifi), getResources()
					.getDrawable(R.drawable.wifi_off), null);
			wifiOnOff.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					wfm.setWifiEnabled(false);
					// "Turning WiFi off"
					wifiOnOff.setText(getResources().getString(
							R.string.jv_advanced_turning_wifi_off));
					wifiOnOff.setEnabled(false);
				}
			});
			wifiNetworks = readScanResults(wfm);
            EinkScreen.PrepareController(null, false);
			adapter.notifyDataSetChanged();
			wifiScan.setEnabled(true);
		} else {
			// "Turn WiFi on"
			wifiOnOff.setText(getResources().getString(
					R.string.jv_advanced_turn_wifi_on));
			wifiOnOff.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.wifi_on), getResources()
					.getDrawable(R.drawable.ci_wifi), getResources()
					.getDrawable(R.drawable.wifi_on), null);
			wifiOnOff.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					wfm.setWifiEnabled(true);
					// "Turning WiFi on"
					wifiOnOff.setText(getResources().getString(
							R.string.jv_advanced_turning_wifi_on));
					wifiOnOff.setEnabled(false);
				}
			});
			wifiNetworks.clear();
            EinkScreen.PrepareController(null, false);
			adapter.notifyDataSetChanged();
			wifiScan.setEnabled(false);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        EinkScreen.setEinkController(prefs);

		ingnoreFs = getResources()
				.getStringArray(R.array.filesystems_to_ignore);

		app = ((ReLaunchApp) getApplicationContext());
		app.setFullScreenIfNecessary(this);
		setContentView(R.layout.advanced_layout);

		createInfo();
		// "Advanced functions, info, etc."
		((TextView) findViewById(R.id.results_title)).setText(getResources()
				.getString(R.string.jv_advanced_title));
		findViewById(R.id.results_btn)
				.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        finish();
                    }
                });

		// Wifi info
		lv_wifi = (ListView) findViewById(R.id.wifi_lv);
		adapter = new WiFiAdapter(this);
		lv_wifi.setAdapter(adapter);

		if (prefs.getBoolean("customScroll", app.customScrollDef)) {
			int scrollW;
			try {
				scrollW = Integer
						.parseInt(prefs.getString("scrollWidth", "25"));
			} catch (NumberFormatException e) {
				scrollW = 25;
			}

			if (addSView) {
				LinearLayout ll = (LinearLayout) findViewById(R.id.wifi_lv_layout);
				final SView sv = new SView(getBaseContext());
				LinearLayout.LayoutParams pars = new LinearLayout.LayoutParams(
						scrollW, ViewGroup.LayoutParams.FILL_PARENT, 1f);
				sv.setLayoutParams(pars);
				ll.addView(sv);
				lv_wifi.setOnScrollListener(new AbsListView.OnScrollListener() {
					public void onScroll(AbsListView view,
							int firstVisibleItem, int visibleItemCount,
							int totalItemCount) {
						sv.total = totalItemCount;
						sv.count = visibleItemCount;
						sv.first = firstVisibleItem;
                        EinkScreen.PrepareController(null, false);
                        sv.invalidate();
					}

					public void onScrollStateChanged(AbsListView view,
							int scrollState) {
					}
				});
				addSView = false;
			}
		} else {
			lv_wifi.setOnScrollListener(new AbsListView.OnScrollListener() {
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
                    EinkScreen.PrepareController(null, false);
				}

				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
				}
			});
		}

		wifiScan = (Button) findViewById(R.id.wifi_scan_btn);
		wifiScan.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				wfm.startScan();
				wifiScan.setEnabled(false);
			}
		});

		// Receive broadcast when scan results are available
		i1 = new IntentFilter();
		i1.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		b1 = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				wifiNetworks = readScanResults(wfm);
				wifiScan.setEnabled(true);
				adapter.notifyDataSetChanged();
			}
		};
		registerReceiver(b1, i1);

		// Receive broadcast when WiFi status changed
		i2 = new IntentFilter();
		i2.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		i2.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		i2.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		i2.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		i2.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		b2 = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				wifiOn = wfm.isWifiEnabled();
				wifiNetworks = readScanResults(wfm);
				wifiScan.setEnabled(true);
				adapter.notifyDataSetChanged();
				updateWiFiInfo();
			}
		};
		registerReceiver(b2, i2);

		wifiOnOff = (Button) findViewById(R.id.wifi_onoff_btn);
		updateWiFiInfo();

		Button wifiSetup = (Button) findViewById(R.id.wifi_setup_btn);
		wifiSetup.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

                if (N2DeviceInfo.EINK_SONY) {
                    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    // SONY PRS-Tx only!
                    final ComponentName cn = new ComponentName(
                            "com.sony.drbd.ebook.NetworkManagerSettings",
                            "com.sony.drbd.ebook.NetworkManagerSettings.NMWirelessSetting");
                    intent.setComponent(cn);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else if (N2DeviceInfo.EINK_NOOK) {
					final Intent intent = new Intent(Intent.ACTION_MAIN, null);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					// NOOK ST only!
					final ComponentName cn = new ComponentName(
							"com.android.settings",
							"com.android.settings.wifi.Settings_Wifi_Settings");
					intent.setComponent(cn);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				} else {
                    final Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
				}
			}
		});

		// Lock button
		final Activity parent = this;
		Button lockBtn = (Button) findViewById(R.id.lock_btn);
		lockBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (PowerFunctions.actionLock(parent)) {
					parent.finish();
				}
			}
		});

		// Reboot button
		rebootBtn = (Button) findViewById(R.id.reboot_btn);
		rebootBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PowerFunctions.actionReboot(parent);
			}
		});

		// Power Off button
		powerOffBtn = (Button) findViewById(R.id.poweroff_btn);
		powerOffBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				PowerFunctions.actionPowerOff(parent);
			}
		});

		// Web info view
		WebView wv = (WebView) findViewById(R.id.webview1);
		final int ntitle1 = 3;
		final int ntitle2 = 8;
		// "<h2><center>Disks/partitions</center></h2><table>"
		String s = "<h3><center>"
				+ getResources().getString(
						R.string.jv_advanced_mount_diskspartitions)
				+ "</center></h3><table>";
		s += "<tr>"
		// + "<td><b>" + sp(ntitle1) + "Mount point" + sp(ntitle2) + "</b></td>"
				+ "<td><b>"
				+ sp(ntitle1)
				+ getResources().getString(
						R.string.jv_advanced_mount_mountpoint)
				+ sp(ntitle2)
				+ "</b></td>"
				// + "<td><b>" + sp(ntitle1) + "FS" + sp(ntitle2) + "</b></td>"
				+ "<td><b>"
				+ sp(ntitle1)
				+ getResources().getString(R.string.jv_advanced_mount_FS)
				+ sp(ntitle2)
				+ "</b></td>"
				// + "<td><b>" + sp(ntitle1) + "total" + sp(ntitle2) +
				// "</b></td>"
				+ "<td><b>"
				+ sp(ntitle1)
				+ getResources().getString(R.string.jv_advanced_mount_total)
				+ sp(ntitle2)
				+ "</b></td>"
				// + "<td><b>" + sp(ntitle1) + "used" + sp(ntitle2) +
				// "</b></td>"
				+ "<td><b>"
				+ sp(ntitle1)
				+ getResources().getString(R.string.jv_advanced_mount_used)
				+ sp(ntitle2)
				+ "</b></td>"
				// + "<td><b>" + sp(ntitle1) + "free" + sp(ntitle2) +
				// "</b></td>"
				+ "<td><b>"
				+ sp(ntitle1)
				+ getResources().getString(R.string.jv_advanced_mount_free)
				+ sp(ntitle2)
				+ "</b></td>"
				// + "<td><b>" + sp(ntitle1) + "RO/RW" + sp(ntitle2) +
				// "</b></td>"
				+ "<td><b>" + sp(ntitle1)
				+ getResources().getString(R.string.jv_advanced_mount_rorw)
				+ sp(ntitle2) + "</b></td>" + "</tr>";
		for (Info i : infos)
			s += "<tr>" + "<td>"
					+ i.mpoint
					+ "</td>"
					+ "<td>"
					+ i.fs
					+ "</td>"
					+ "<td>"
					+ i.total
					+ "</td>"
					+ "<td>"
					+ i.used
					+ "</td>"
					+ "<td>"
					+ i.free
					+ "</td>"
					// + "<td>" + (i.ro ? "RO" : "RW") + "</b></td>"
					+ "<td>"
					+ (i.ro ? getResources().getString(
							R.string.jv_advanced_mount_ro) : getResources()
							.getString(R.string.jv_advanced_mount_rw))
					+ "</b></td>" + "</tr>";
		s += "</table>";
		wv.loadDataWithBaseURL(null, s, "text/html", "utf-8", null);

		ScreenOrientation.set(this, prefs);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
        unregisterReceiver(b1);
        unregisterReceiver(b2);
	}


	@Override
	protected void onResume() {
		super.onResume();
        EinkScreen.setEinkController(prefs);
		app.generalOnResume(TAG, this);
	}

}
