package ca.wise.geoserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import kong.unirest.ObjectMapper;

public class ProtobufObjectMapper implements ObjectMapper {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T readValue(String value, Class<T> valueType) {
		if (Message.class.isAssignableFrom(valueType)) {
			try {
				Method m = valueType.getMethod("newBuilder");
				Message.Builder builder = (Message.Builder)m.invoke(null);
				JsonFormat.parser()
					.ignoringUnknownFields()
					.merge(value, builder);
				return (T)builder.build();
			}
			catch (NoSuchMethodException e) { }
			catch (SecurityException e) { }
			catch (IllegalAccessException e) { }
			catch (IllegalArgumentException e) { }
			catch (InvocationTargetException e) { }
			catch (InvalidProtocolBufferException e) { }
		}
		return null;
	}

	@Override
	public String writeValue(Object value) {
		if (value instanceof MessageOrBuilder) {
			try {
				return JsonFormat.printer()
						.includingDefaultValueFields()
						.print((MessageOrBuilder)value);
			}
			catch (InvalidProtocolBufferException e) { }
		}
		return null;
	}
}
