package p1;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	public void mA1Moved(@NonNull B this, @NonNull A a) {
		mB1();
		System.out.println(this + "j");
		System.out.println(a.mA2());
	}
}