package p;
@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
class A{	
}
class B extends A{

	public void foo1(@NonNull B this) {
		System.out.println("foo1");
	}

	public void foo2(@NonNull B this, String s) {
		System.out.println(s);
	}
}