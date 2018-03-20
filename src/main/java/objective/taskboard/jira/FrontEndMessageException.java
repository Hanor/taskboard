package objective.taskboard.jira;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import retrofit.RetrofitError;
import retrofit.mime.TypedByteArray;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FrontEndMessageException extends RuntimeException {

    private static final long serialVersionUID = -3096553868407268805L;

    public FrontEndMessageException(String message) {
        super(message);
    }

    public FrontEndMessageException(RetrofitError e) {
        super(parseExceptionMessage(e));
    }

    private static String parseExceptionMessage(RetrofitError e) {
        if (e.getResponse().getStatus() == HttpStatus.NOT_FOUND.value())
            return "NOT FOUND";
        List<String> errors = new ArrayList<>();
        errors.addAll(getErrorsFromResponse(e));
        errors.addAll(getErrorsMessagesFromResponse(e));
        return errorsListToString(errors);
    }

    private static String errorsListToString(List<String> errors) {
        return String.join("\n", errors);
    }

    private static String getBodyAsString(RetrofitError error) {
        String mimetype = error.getResponse().getBody().mimeType();
        Matcher matcher = Pattern.compile("charset=(.+)").matcher(mimetype);
        String encoding = matcher.find() ? matcher.group(1) : "UTF-8";
        try {
            return new String(((TypedByteArray) error.getResponse().getBody()).getBytes(), encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    private static List<String> getErrorsFromResponse(RetrofitError error) {
        List<String> errors = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(getBodyAsString(error));
            JSONObject jsonErrors = (JSONObject) jsonObj.get("errors");
            Iterator<?> itErrors = jsonErrors.keys();
            while(itErrors.hasNext()) {
                String key = (String) itErrors.next();
                errors.add((String) jsonErrors.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        return errors;
    }

    private static List<String> getErrorsMessagesFromResponse(RetrofitError error) {
        List<String> errors = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(getBodyAsString(error));
            JSONArray jsonErrors = (JSONArray) jsonObj.get("errorMessages");
            for(int i = 0; i < jsonErrors.length(); i++)
                errors.add(jsonErrors.getString(i));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        return errors;
    }

}