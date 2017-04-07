package android.csulb.edu.crisisconnect.Calling;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.koushikdutta.async.AsyncNetworkSocket;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HTTPServerService extends Service {
    private AsyncHttpServer server = new AsyncHttpServer();
    private static final String TAG = "HTTPServerService";

    private List<WebSocket> _sockets = new ArrayList<WebSocket>();

    public HTTPServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        server.post("/", new HttpServerRequestCallback() {
                    @Override
                    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                        Log.d(TAG, request.toString());
                        Log.d(TAG, request.getBody().get().getClass().toString());
                        if (request != null) {
                            if (request.getBody() instanceof JSONObjectBody) {
                                JSONObject reqJSON = ((JSONObjectBody) request.getBody()).get();
                                if (reqJSON != null) {
                                    Intent i = new Intent(Util.INTENT_FILTER_SERVICE_ACTIVITY);
                                    try {
                                        switch (reqJSON.getInt(Util.KEY_OPERATION)) {
                                            case Util.OPERATION_TYPE_REQUEST_CALL: {
                                                //send broascast to activities and show them a accept/reject interface
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_NEW_INCOMING_CALL);
                                                i.putExtra(Util.KEY_INTENT_FILTER_OTHER_USERNAME, reqJSON.getString(Util.KEY_OTHER_USERNAME));
                                            }
                                            break;
                                            case Util.OPERATION_TYPE_REQUEST_IMAGE: {
                                                //send broascast to activities and show them a accept/reject interface
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_NEW_INCOMING_IMAGE);
                                                i.putExtra(Util.KEY_INTENT_FILTER_OTHER_USERNAME, reqJSON.getString(Util.KEY_OTHER_USERNAME));
                                                i.putExtra(Util.KEY_INTENT_FILTER_IMAGE,reqJSON.getString(Util.KEY_Image));
                                            }
                                            break;
                                            case Util.OPERATION_TYPE_END_CALL: {
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_CALL_ENDED);
                                                //TODO: Do something
                                            }
                                            break;
                                            case Util.OPERATION_TYPE_ACCEPT_CALL: {
                                                //TODO: Do something
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_CALL_ACCEPTED);

                                                Log.d(TAG, "CAllee accepted call");
                                            }
                                            break;
                                            case Util.OPERATION_TYPE_ACCEPT_IMAGE: {
                                                //TODO: Do something
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_IMAGE_ACCEPTED);

                                                Log.d(TAG, "CAllee accepted Image");
                                            }
                                            break;
                                            case Util.OPERATION_TYPE_REJECT_CALL: {
                                                i.putExtra(Util.KEY_INTENT_FILTER_REASON, Util.INTENT_FILTER_REASON_CALL_REJECTED);
                                                Log.d(TAG, "CAllee rejectedcall");
                                            }
                                            break;
                                            default:
                                                break;
                                        }

                                        AsyncNetworkSocket socketConverted = com.koushikdutta.async.Util.getWrappedSocket(request.getSocket(), AsyncNetworkSocket.class);
                                        String callerIP = socketConverted.getRemoteAddress().getAddress().getHostAddress();
                                        i.putExtra(Util.KEY_OTHER_IP, callerIP);

                                        LocalBroadcastManager.getInstance(HTTPServerService.this).sendBroadcast(i);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }


                    }
                }

        );

// listen on port 5000
        server.listen(Util.HTTP_PORT);
    }
}
