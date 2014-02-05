package p;

import org.eclipse.jdt.annotation.NonNull;
public class A implements Inter1{

	public Integer getArea(@NonNull Integer length) {
		return new Integer(length * length);
	}
}