package p1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
@Target({ ElementType.TYPE_USE})
@interface Nullable {

}
public class A {

	public void mA1(@Nullable A this, @NonNull B b) {
		b.mA1Moved(this);
	}
	
	public void mA2() {}

}