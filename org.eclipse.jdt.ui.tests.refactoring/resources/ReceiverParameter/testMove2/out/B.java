package p1;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	public void mA1Moved(@NonNull B this, @Nullable A a) {
		mB1();
		System.out.println(a + "j");
	}
}