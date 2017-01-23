package org.openntf.xrest.xsp.exec.convertor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.openntf.xrest.xsp.model.MapJsonTypeProcessor;

import com.ibm.commons.util.io.json.JsonObject;

import lotus.domino.DateTime;
import lotus.domino.Item;
import lotus.domino.NotesException;

public class DateTimeMapJsonTypeProcessor implements MapJsonTypeProcessor {

	@Override
	public void processItemToJsonObject(Item item, JsonObject jo, String jsonPropertyName) throws NotesException {
		DateTime dtCurrent = item.getDateTimeValue();
		Date javaDate = dtCurrent.toJavaDate();
		jo.putJsonProperty(jsonPropertyName, buildISO8601Date(javaDate));
	}

	@Override
	public void processValuesToJsonObject(List<?> values, JsonObject jo, String jsonPropertyName) throws NotesException {
		if (values != null && !values.isEmpty()) {
			Object value = values.get(0);
			if (value instanceof DateTime) {
				Date dtCurrent = ((DateTime) value).toJavaDate();
				jo.putJsonProperty(jsonPropertyName, buildISO8601Date(dtCurrent));
			}
		}
	}

	private String buildISO8601Date(Date javaDate) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

		TimeZone tz = TimeZone.getTimeZone("UTC");

		df.setTimeZone(tz);

		String output = df.format(javaDate);

		int inset0 = 9;
		int inset1 = 6;

		String s0 = output.substring(0, output.length() - inset0);
		String s1 = output.substring(output.length() - inset1, output.length());
		String result = s0 + s1;
		result = result.replaceAll("UTC", "+00:00");
		return result;
	}
	
	public static Date parse( String input ) throws java.text.ParseException {

        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );
        
        //this is zero time so we need to add that TZ indicator for 
        if ( input.endsWith( "Z" ) ) {
            input = input.substring( 0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;
        
            String s0 = input.substring( 0, input.length() - inset );
            String s1 = input.substring( input.length() - inset, input.length() );

            input = s0 + "GMT" + s1;
        }
        
        return df.parse( input );
        
    }
}
