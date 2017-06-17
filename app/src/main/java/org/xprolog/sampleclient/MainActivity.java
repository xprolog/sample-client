package org.xprolog.sampleclient;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    /** Messenger for communicating with the service. */
    private Messenger mService;

    /** Flag indicating whether we have called bind on the service. */
    private boolean mBound;

    /** Name of run configuration associated with current process. */
    private String mConfigName;

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
            onMenuRun();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    private EditText mInputEditText;
    private TextView mOutputTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputEditText = (EditText) findViewById(R.id.edit_text);
        mInputEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if (text.indexOf('\n') >= 0) {
                    sendMessage(ServiceConstants.MSG_APPEND, text.getBytes());
                    mInputEditText.getText().clear();
                }
            }
        });
        mInputEditText.requestFocus();

        mOutputTextView = (TextView) findViewById(R.id.text_view);
        mOutputTextView.setMovementMethod(new ScrollingMovementMethod());

        bind();
    }

    @Override
    protected void onDestroy() {
        unbind();
        super.onDestroy();
    }

    private void bind() {
        try {
            Intent intent = new Intent(ServiceConstants.ACTION);
            intent.setPackage(ServiceConstants.PACKAGE);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (SecurityException e) {
            // ignore
        }
    }

    private void unbind() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
            mInputEditText.getText().clear();
            mOutputTextView.setText(null);
            cancelToast();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_run)
            onMenuRun();
        return super.onOptionsItemSelected(item);
    }

    private void onMenuRun() {
        sendMessage(ServiceConstants.MSG_LIST, new byte[0]);
    }

    private void run(String name) {
        mInputEditText.getText().clear();
        mOutputTextView.setText(null);
        sendMessage(ServiceConstants.MSG_TERMINATE, new byte[0]);
        setTitle(R.string.app_name);
        mConfigName = name;
        sendMessage(ServiceConstants.MSG_RUN, name.getBytes());
    }

    /**
     * Handler of incoming messages from the service.
     */
    @SuppressLint("HandlerLeak")
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceConstants.MSG_LIST:
                    onMessageList(msg);
                    break;
                case ServiceConstants.MSG_APPEND:
                    onMessageAppend(msg);
                    break;
                case ServiceConstants.MSG_TRACE:
                    onMessageTrace(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private void onMessageList(Message msg) {
        byte[] bytes = msg.getData().getByteArray(ServiceConstants.ARG1);
        assert bytes != null;
        String text = new String(bytes);
        if (text.isEmpty()) {
            showToast(getResources().getString(R.string.no_config_to_run));
            return;
        }
        final String[] configNames = text.split("\n");
        if (configNames.length == 1) {
            run(configNames[0]);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.run_configuration)
                .setItems(configNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        run(configNames[item]);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                    }});
        cancelToast();
        builder.show();
    }

    private void onMessageAppend(Message msg) {
        byte[] bytes = msg.getData().getByteArray(ServiceConstants.ARG1);
        assert bytes != null;
        if (bytes.length == 0)
            setTitle(mConfigName);
        mOutputTextView.append(new String(bytes));
    }

    private void onMessageTrace(Message msg) {
        byte[] bytes = msg.getData().getByteArray(ServiceConstants.ARG1);
        assert bytes != null;
        showToast(new String(bytes));
    }

    // Create and send a message to the service, using a supported 'what' value

    private void sendMessage(int what, byte[] bytes) {
        if (mBound) {
            Message message = Message.obtain(null, what, 0, 0);
            message.replyTo = mMessenger;
            Bundle bundle = new Bundle();
            bundle.putByteArray(ServiceConstants.ARG1, bytes);
            message.setData(bundle);
            try {
                mService.send(message);
            } catch (RemoteException e) {
                // The service is dead.
                mBound = false;
            }
        }
    }

    private Toast mToast;

    private void cancelToast() {
        if (mToast != null)
            mToast.cancel();
    }

    private void showToast(String text) {
        cancelToast();
        mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

}
