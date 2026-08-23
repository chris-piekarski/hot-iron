package hotiron.mvc;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Class to represent arbitrary value with notification to observers on value change.
 * Used in MVC as a model. 
 * 
 * @param <T>
 */
public class ModelValue<T> {
	
	public static class ModelValueInt extends ModelValue<Integer>{
		protected final boolean isBounded;
		protected final int min, max, step;
		public ModelValueInt(String name, int initialValue) {
			super(name, initialValue);
			isBounded	= false;
			step = min = max = 0;
		}
		
		public ModelValueInt(String name, int initialValue, int step, int min, int max) {
			super(name, initialValue);
			isBounded	= true;
			this.min	= min;
			this.max	= max;
			this.step	= step;
		}
		
		public int getMax() {
			return max;
		}
		
		public int getMin() {
			return min;
		}
		
		public int getStep() {
			return step;
		}
		
		@Override
		public void setValue(Integer value) {
			if (isBounded() && (value < min || value > max))
				throw new IllegalStateException("New value of '"+name+"' '"+value+"' is outside of bounds <"+min+", "+max+">");
			super.setValue(value);
		}
		
		public boolean isBounded() {
			return isBounded;
		}
	}
	
	public static class ModelValueBoolean extends ModelValue<Boolean>{
		public ModelValueBoolean(String name, Boolean initialValue) {
			super(name, initialValue);
		}
	}
	
	private T value;
	protected final String name;
	private final CopyOnWriteArrayList<Consumer<T>> listeners = new CopyOnWriteArrayList<Consumer<T>>();

	public ModelValue(String name, T initialValue) {
		this.value	= initialValue;
		this.name	= name;
	}
	
	
	public void addListener(Consumer<T> listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener");
		listeners.add(listener);
	}
	
	public void addListener(Runnable listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener");
		listeners.add(value -> listener.run());
	}
	
	
	
	public void setValue(T value) {
		if (value == null || this.value == null) {
			if (value == this.value)
				return;
		}
		else if (this.value.equals(value)) {
			return;
		}
		this.value	= value;
		
		callObservers();
	}
	
	public void callObservers() {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).accept(value);
	}
	
	public T getValue() {
		return value;
	}
	
	@Override public String toString() {
		return name;
	}
}