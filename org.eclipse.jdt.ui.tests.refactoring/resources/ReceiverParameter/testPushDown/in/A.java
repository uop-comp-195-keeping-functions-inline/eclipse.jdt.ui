package p;
@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
class A{
	public void foo1(@NonNull A this) {
		System.out.println("foo1");
	}
	public void foo2(@NonNull A this, String s) {
		System.out.println(s);
	}	
}
class B extends A{
}