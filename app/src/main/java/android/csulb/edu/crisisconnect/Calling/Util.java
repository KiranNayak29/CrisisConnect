package android.csulb.edu.crisisconnect.Calling;

public class Util {
    public static final String KEY_OPERATION="keyrequesttype";
    //    public static final String KEY_DECISION="keydecision";
    public static final String KEY_CALLER_IP="keycallerip";
    public static final String KEY_OTHER_IP="keyotherip";
    public static final String KEY_OTHER_USERNAME="keyotherusername";
    public static final String KEY_OUTGOING="keyoutgoing";

    public static  final int OPERATION_TYPE_REQUEST_CALL=0;
    public static  final int OPERATION_TYPE_ACCEPT_CALL=1;
    public static  final int OPERATION_TYPE_REJECT_CALL=2;
    public static  final int OPERATION_TYPE_END_CALL=3;
    public static  final int OPERATION_TYPE_REQUEST_IMAGE=4;
    public static  final int OPERATION_TYPE_ACCEPT_IMAGE=5;
//    public static  final int DECISION_TYPE_ACCEPT=2;
//    public static  final int DECISION_TYPE_REJECT=3;

    public static final String INTENT_FILTER_SERVICE_ACTIVITY ="voiptrialfilter";
    public static final String KEY_INTENT_FILTER_REASON ="filterdecision";
    public static final String KEY_INTENT_FILTER_OTHER_USERNAME ="filterotherusername";
    public static final String KEY_INTENT_FILTER_IMAGE ="filterimage";
    public static final int INTENT_FILTER_REASON_NEW_INCOMING_CALL =0;
    public static final int INTENT_FILTER_REASON_CALL_ACCEPTED =1;  //accepted by other party
    public static final int INTENT_FILTER_REASON_CALL_REJECTED =2;  //rejected by other party
    public static final int INTENT_FILTER_REASON_CALL_ENDED =3;
    public static final int INTENT_FILTER_REASON_NEW_INCOMING_IMAGE =4;
    public static final int INTENT_FILTER_REASON_IMAGE_ACCEPTED =5;
    public static final int INTENT_FILTER_REASON_NO_REASON =99;

    //shared preferences
    public static final String KEY_PREFS_USERNAME="usernamekey";
    public static final String KEY_PREFS_PASSWORD="passwordkey";
    public static final String KEY_PREFS_EMAIL="emailkey";
    public static final String KEY_Image="image";
    public static final String KEY_PREFS_GENDER="genderkey";
    public static final String KEY_PREFS_DESIG="desigkey";
    public static final String KEY_PREFS_DEPT="deptkey";

    public static final String GENDER_MALE = "malegender";
    public static final String GENDER_FEMALE = "femalegender";
    public static final int HTTP_PORT = 5000;
    public static final int RTSP_PORT = 8086; //using the default port that libstreaming uses(8086)
    public static String PROTOCOL_HTTP = "http://";
    public static String PROTOCOL_RTSP = "rtsp://";
}
