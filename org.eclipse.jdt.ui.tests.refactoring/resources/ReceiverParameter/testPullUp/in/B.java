package p;

import java.util.List;
class B extends A{
	void foo1(@NonNull  B this, List l){}
	void foo2(@NonNull  B this, String s){}
}