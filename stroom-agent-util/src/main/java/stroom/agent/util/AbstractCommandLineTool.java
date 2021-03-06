package stroom.agent.util;

import stroom.agent.util.zip.HeaderMap;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Base class for command line tools that handles setting a load of args on the
 * program as name value pairs
 */
public abstract class AbstractCommandLineTool {
	
	private static class Example extends AbstractCommandLineTool {
		@Override
		public void run() {
			throw new RuntimeException();
		}
	}

	private HeaderMap map;
	private List<String> validArguments;
	private int maxPropLength = 0;

	public abstract void run();

	protected void checkArgs() {

	}

	protected void failArg(String arg, String msg) {
		throw new RuntimeException(arg + " - " + msg);
	}

	public void init(String[] args) throws Exception {
		map = new HeaderMap();
		validArguments = new ArrayList<String>();

		map.loadArgs(args);

		BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass());

		for (PropertyDescriptor field : beanInfo.getPropertyDescriptors()) {
			if (field.getWriteMethod() != null) {
				if (field.getName().length() > maxPropLength) {
					maxPropLength = field.getName().length();
				}
				if (map.containsKey(field.getName())) {
					validArguments.add(field.getName());
					field.getWriteMethod().invoke(this, getAsType(field));
				}
			}
		}

		checkArgs();
	}

	private Object getAsType(PropertyDescriptor descriptor) {
		Class<?> propertyClass = descriptor.getPropertyType();
		if (propertyClass.equals(String.class)) {
			return map.get(descriptor.getName());
		}
		if (propertyClass.equals(Boolean.class)
				|| propertyClass.equals(Boolean.TYPE)) {
			return Boolean.parseBoolean(map.get(descriptor.getName()));
		}
		if (propertyClass.equals(Integer.class)
				|| propertyClass.equals(Integer.TYPE)) {
			return Integer.parseInt(map.get(descriptor.getName()));
		}
		if (propertyClass.equals(Long.class) || propertyClass.equals(Long.TYPE)) {
			return Long.parseLong(map.get(descriptor.getName()));
		}
		throw new RuntimeException(
				"AbstractCommandLineTool does not know about properties of type "
						+ propertyClass);
	}

	public void doMain(String[] args) throws Exception {
		init(args);
		run();
	}

	public void traceArguments(PrintStream printStream) {
		try {
			doTraceArguments(printStream);
		} catch (Exception ex) {
			printStream.println(ex.getMessage());
		}
	}

	private void doTraceArguments(PrintStream printStream) throws Exception {
		BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass());
		for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			// Only do properties with getters
			if (pd.getWriteMethod() != null) {
				// Simple getter ?
				String suffix = " (default)";
				if (map.containsKey(pd.getName())) {
					suffix = " (arg)";
				}
				String value = "";

				if (pd.getReadMethod() != null
						&& pd.getReadMethod().getParameterTypes().length == 0) {
					value = String.valueOf(pd.getReadMethod().invoke(this));
				} else {
					// No simple getter
					Field field = null;
					try {
						field = this.getClass().getDeclaredField(pd.getName());
					} catch (NoSuchFieldException nsfex) {
						// Ignore
					}
					if (field != null) {
						field.setAccessible(true);
						value = String.valueOf(field.get(this));
					} else {
						value = "?";

					}
				}
				printStream.println(StringUtils.rightPad(pd.getName(),
						maxPropLength) + " = " + value + suffix);
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		Example example = new Example();
		example.doMain(args);
	}

}
