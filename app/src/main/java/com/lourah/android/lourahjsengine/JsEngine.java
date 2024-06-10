/**
 * JsEngine: Make Android scriptable with javascript
 *
 * @author fred.oden@gmail.com
 */

package com.lourah.android.lourahjsengine;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.*;
import java.nio.charset.StandardCharsets;
//import com.google.android.material.snackbar.Snackbar;

/**
 * The encapsulating Android activity for the possibles javascript applications
 * Each js application is located in a subdirectory of ${EXTERNAL_STORAGE}/LourahJsEngine
 * The name of the subdirectory is the name of the application which should contain, at least,
 * a valid javacript script in a file called "index.js"
 * The call of index.js interpretation is encapsulated by the javascript script starter.js located
 * in the assets Lourah/JsEngine directory.
 * starter.js : construct a specific environment for the javascript application in a unic object
 *              called Lourah.
 *              This object Lourah can be succesivelly be enriched with some javascript
 *              "library" or "modules"
 *              Lourah belongs to the javascript world...
 */
public class JsEngine
    extends Activity {

  private View contentView;
  private Js js;

  /**
   *
   * @return the current content view
   */
  public View getContentView() {
    return contentView;
  }
  
  public TextView tv;

  /**
   * creates a view that contains:
   *    a TextView for notification purpose
   *    a set of Button each dynamically generated by exploring
   *    ${EXTERNAL_STORAGE}/LourahJsEngine subdirectories containing a index.js file
   *    each button text is set with the subdirectory name (name of application)
   *    and onClickListener is meant to execute the interpretation of index.js encapsulated
   *    by the asset Lourah/JsEngine/starter.js
   */
  private void createContentView() {
    ScrollView sv = new ScrollView(this);
    sv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    LinearLayout ll = new LinearLayout(this);
    ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    ll.setOrientation(LinearLayout.VERTICAL);
    ll.setBackgroundColor(0x7f7fffff);
    sv.addView(ll);
    contentView = sv;
    tv = new TextView(this);
        tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    //File fJsEngine = new File(Environment.getExternalStorageDirectory().toString() + "/LourahJs");
    File fJsEngine = new File(getRootDir().toString() + "/LourahJs");
    File[] files = fJsEngine.listFiles();
    try {
      for(File file : files) {
        if (file.isDirectory()) {
           final File index = new File(file.getAbsolutePath()
                                 + "/index.js"
                                 );
           if (index.exists() && index.isFile()) {
             Button bIndex = new Button(this);
             bIndex.setText(file.getName());
             
               
             ll.addView(bIndex);
             bIndex.setOnClickListener(
                new View.OnClickListener() {
                  public void onClick(View view) {
                    //String indexPath = Environment.getExternalStorageDirectory()
                    String indexPath = getRootDir()
                            + "/LourahJs"
                            + "/"
                            + ((Button) view).getText()
                            + "/"
                            + "index.js"
                    ;
                    try {
                      // Encapsulate index.js in asset Lourah/JsEngine/starter.js
                      HashMap<String, String> starterMacros = new HashMap<>();
                      starterMacros.put("@@@RHINO_VERSION@@@", "1.7.15");
                      starterMacros.put("@@@GENERATED@@@", "20240607");
                      starterMacros.put("@@@JS_APP_NAME@@@", ((Button)view).getText().toString());
                      starterMacros.put("@@@INTERNAL_STORAGE_DIRECTORY@@@", getExternalFilesDir("LourahJsEngine" /* Environment.getDataDirectory().getAbsolutePath() */).getAbsolutePath().toString());
                      //starterMacros.put("@@@INTERNAL_STORAGE_DIRECTORY@@@", getFilesDir().toString());
                      starterMacros.put("@@@EXTERNAL_STORAGE_DIRECTORY@@@", getRootDir().toString());
                      // jsFrameworkDirectory to be configurable in a future version ?
                      starterMacros.put("@@@JS_FRAMEWORK_DIRECTORY@@@", "LourahJs");
                                        starterMacros.put("@@@INDEX_PATH@@@", indexPath);
                      String script = path2String(indexPath);
                      starterMacros.put("@@@SCRIPT@@@", script);
                      String starter = asset2String("Lourah/JsEngine/starter.js");
                      for(String k : starterMacros.keySet()) {
                                            // Debugging Macros
                                            if (!k.equals("@@@SCRIPT@@@")) tv.append(k + "::" + starterMacros.get(k) + "\n");
                        starter = starter.replace(k, starterMacros.get(k));
                      }
                      //tv.append(indexPath+"::<<<\n"+starter + "\n>>>");
                      Js.JsObject o =
                              js.eval(starter, indexPath);
                      if(!o.ok) {
                        reportError("LourahJsEngine::Loading::"
                        + ((Button)view).getText()
                        + "::"
                        + o.s
                        );
                      }
                    } catch(Exception e) {
                      reportError("LourahJsEngine::starter::exception::"
                              + ((Button)view).getText()
                              + "::"
                              + e.getMessage()
                      );
                    }
                  }
                }
             );
           }
        }
      }
    } catch(Exception e) {
      reportError("cannot list JsEngine::" + e);
    }
    
    
    ll.addView(tv);
    
  }


  /**
   * To make possible to develop specific errorReporter from javascript.
   */
  public interface ErrorReporter {
     void report(String m);
  }
  
  private JsEngine self = this;

  /**
   * This default reporter is based on Toast and Log
   * Toast is an issue and sould be replaced with a more convenient
   * way to notify error(s) to the user.
   */
  public ErrorReporter errorReporter = new ErrorReporter() {
    public void report(String m) {
      Toast.makeText(self, m, Toast.LENGTH_LONG)
    .show();
      android.util.Log.e("LourahJsEngine", m);
    }
  };

  /**
   * As notification appears on the user interface, the report is always
   * executed in the UI Thread of android. (As javascript application can be
   * multithreaded).
   * @param error  message
   */
  public void reportError(String error) {
    final String m = error;
    runOnUiThread(new Runnable(){
      @Override
      public void run() {
        errorReporter.report("reportError::" + m);
      }
    });
  }

  /**
   * Not yet implemented
   * @param warning
   */
  public void reportWarning(String warning) {
    
  }


  static boolean first = true;

  /**
   * Create this activity
   * @param savedInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (first) {
      createContentView();
      setContentView(contentView);
      js = new Js(this);
      //first = false;
    } else {
      setContentView(getContentView());
    }
  }

  //public static Charset encoding = StandardCharsets.UTF_8;
  public static File root = null;


  public File getRootDir() {
if (false && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
    // Do something for lollipop and above versions
    //File file = getApplicationContext().getFilesDir();
    File file = getFilesDir();
    tv.append("FilesDir::"+file+"\n");
    if (!file.exists()) {
	    file.mkdir();
    	    tv.append("mkdir::"+file);
    }
    /**/
    return file;
} else{
    // do something for phones running an SDK before lollipop
    return Environment.getExternalStorageDirectory();
}}
  /**
   * Common method to load a source code (utf-8) from an InputStream
   * @param is to read
   * @return full source code content
   */
  public String inputStream2String(InputStream is) {
    StringBuffer sb = new StringBuffer();
    try {
       BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
       int c = 0;
       while((c = br.read()) != -1) {
         sb.append((char)c);
       }
       br.close();
       
    } catch (Exception ioe) {
     reportError("inputStream2string::" + ioe);
    } 
    return sb.toString();
  }

  /**
   * read source code from asset file
   * @param asset filename
   * @return full source code content
   */
  public String asset2String(String asset) {
    String ret = "";
    try {
     ret = inputStream2String(getAssets().open(asset));
     } catch(IOException ioe) {
       reportError("asset2String::" +ioe);
     }
     return ret;
  }

  /** 
   * read source code from external storage file
   * @param path  of the file
   * @return full source code content
   */
  public String path2String(String path) {
    String ePath = path.replaceAll(" ", "%20");
    try {
      //File f = new File(path);
      //return inputStream2String(new FileInputStream(f));
            //String ePath = URLEncoder.encode(path.replaceAll(" ", "%20"), StandardCharsets.UTF_8.toString());
            URI uri = URI.create(ePath);
            if (uri.getScheme() == null) uri = URI.create("file://" + ePath);
            if (uri.getScheme().equals("file")) {
                return inputStream2String(uri.toURL().openStream());
                }
            // Asynchronous call on network
            class Cb implements Runnable {
                private volatile String s;
                private volatile boolean bSuccess;
                private volatile IOException ioException;
                private volatile URL url;
                @Override
                public void run () {
                    try {
                        s = inputStream2String(url.openStream());
                        bSuccess = true;
                    } catch (IOException ioe) {
                        bSuccess = false;
                        ioException = new IOException(new String("Error::" + ePath + "::" + ioe.getMessage()));
                    }
                }
                public String getContent() throws IOException {
                    if (!bSuccess) throw ioException;
                    return s;
                }
                
                public void setURL(URL u) {
                    url = u;
                }
            }
            Cb cb = new Cb();
            cb.setURL(uri.toURL());
            Thread t = new Thread(cb);
            t.start();
            t.join();
            return cb.getContent();
    } catch (IOException ioe) {
      return "path2String" + ePath + "::" + ioe.getMessage();
    } catch (InterruptedException ie) {
        return "path2String::" + ePath + "::" + ie.getMessage();
    }
  }

  @Deprecated
  public String file2String_deprecated(String filename) {
    
    //if (root == null) root = Environment.getExternalStorageDirectory();
    if (root == null) root = getRootDir();
    return path2String(root + filename);
  }

  public Js getJs() {
    return js;
  }


  /**
   * Called from javascript for modular purpose
   * @param scriptName
   * @return execution status or value
   */
  public Js.JsObject importScript(String scriptName) {
    Js.JsObject o;
    o = js.eval(path2String(scriptName), scriptName);
    if (!o.ok) {
      reportError("importScript::" + o.s);
    }
    return o;
  }

  private static boolean checkPermission = true;
  private AlertDialog permissionDialog;

  @Override
  public void onResume() {
    super.onResume();
    // @@@ PERMISSION CHECK 20201022
    if(Build.VERSION.SDK_INT >= 23) {
      if (checkPermission) {
        checkPermission = false;
        try {
          askForPermissions();
        } catch (Exception e) {
          //tv.setText(e.getMessage() + stringifyStackTrace(e));
        }
      }
    }
    androidHandler("onResume");
  }

  @Override
  public void onStop() {
    super.onStop();
    //reportError("ON STOP");
    androidHandler("onStop");
  }
  
  @Override
  public void onDestroy(){
    super.onDestroy();
    androidHandler("onDestroy");
  }
  
  @Override
  public void onPause() {
    super.onPause();
    androidHandler("onPause");
  }
  
  @Override
  public void onStart() {
    super.onStart();
    androidHandler("onStart");
  }
  
  @Override
  public void onRestart() {
    super.onRestart();
    androidHandler("onRestart");
  }
  
  @Override
  public void onBackPressed() {
    String script = "Lourah.jsFramework.onBackPressed();";
    Js.JsObject o =
           js.eval(script, "JsEngine.java");
           if(!o.ok) {
                reportError("LourahJsEngine::onBackPressed::"
                    + o.s
                   );
                return;
           }
    if (o.s.equals("false")) {
      super.onBackPressed();
    }
  }

  /**
   * To redirect Activity events to javascript
   * @param onEvent (name of Android activity event) @see Lourah/JsEngine/starter.js
   */
  protected void androidHandler(String onEvent) {
    String script = "(function() {try {var handler = (Lourah !== undefined)?Lourah.jsEngine.getAndroidOnHandler('" + onEvent + "'):undefined;"
           + "if (handler !== undefined) { handler(); }} catch(e){}})()";
    Js.JsObject o =
           js.eval(script, "JsEngine.java");
           if(!o.ok) {
                reportError("LourahJsEngine::androidHandler::"
                    + onEvent + "::"
                    + o.s
                   );
                return;
           }
  }

  /**
   * for display purpose
   * @param e exception error
   * @return
   */
  private String stringifyStackTrace(Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return "{" + sw.toString() + "}";
  }

  /*
       PERMISSIONS HANDLING BELOW ... TO MAKE IT CLEAN
   */

  /**
   * Code fossilization, don't know if it works on older Android versions ...
   */
  protected void checkPermissions() {
    if (checkPermission && Build.VERSION.SDK_INT >= 23) {
      Activity activity = this; // /getParentActivity();
      if (activity != null) {
        checkPermission = false;
        if (activity.checkSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED
            || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
          if (activity.shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("LourahJsEngine");
            builder.setMessage("Grant Internet");
            builder.setPositiveButton("Grant", null);
            permissionDialog = builder.create();
            permissionDialog.show();
          } else if (activity.shouldShowRequestPermissionRationale(
              Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("LourahJsEngine");
            builder.setMessage("Grant writeExternalStorage");
            builder.setPositiveButton("Grant", null);
            permissionDialog = builder.create();
            permissionDialog.show();
          } else {
            //if (checkPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            try {
              askForPermissions();
            } catch (Exception e) {
              Toast.makeText(this, "CheckPermissions::" + e.getMessage(), Toast.LENGTH_LONG);
              return;
            }
          }
        }
      }
    }
  }

  /**
   * Permissions handling over Android M ...
   */
  @TargetApi(Build.VERSION_CODES.M)
  @SuppressLint("NewApi")
  private void askForPermissions() {
    ArrayList<String> permissions = new ArrayList<>();
    //if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      //  != PackageManager.PERMISSION_GRANTED) {
      permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
      permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      permissions.add(Manifest.permission.INTERNET);
      permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
      permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    //}
    String[] items = permissions.toArray(new String[permissions.size()]);
    requestPermissions(items, 100);
  }

  
  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    String permissions[],
    int [] grantResults
  ) {
    String s = "";
    switch(requestCode) {
      case 100:
           for(int i = 0; i < grantResults.length; i++){
              s += permissions[i] +":" +
             ((grantResults[i] == PackageManager.PERMISSION_GRANTED)
             ?"ok":"ko") + "\n";
           } 
           tv.append(s);
           
           break;
    }
    
  }
  
  /*
  @Override
  protected void onDialogDismiss(Dialog dialog) {
      super.onDialogDismiss(dialog);
      if (permissionDialog != null && dialog == permissionDialog && this != null) {
          askForPermissons();
      }
  }
  */

}
