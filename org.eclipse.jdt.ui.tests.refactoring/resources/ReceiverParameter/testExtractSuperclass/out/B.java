package p;

public class B {

	public B() {
		super();
	}

	void foo1(@NonNull B this) {
		System.out.println("foo1");
	}

	void foo2(@NonNull B this, String s) {
		System.out.println("foo2");
	}

}