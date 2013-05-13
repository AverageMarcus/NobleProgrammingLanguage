
public class NblValue<T> {
	public enum NblValueType {
		INTEGER,
		STRING,
		LITERAL,
		BOOL,
		CHAR,
		COLOUR,
		FLOAT,
		NULL,
		ARRAY,
	};

	NblValueType type;
	T value;

	public NblValue(NblValueType _t, T _v) {
		type = _t;
		value = _v;
	}

	public NblValueType getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	public String toString() {
		return value.toString();
	}
}
