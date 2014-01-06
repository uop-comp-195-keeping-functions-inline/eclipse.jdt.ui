// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class Test {
	public static final FI f1 = C::m; 	
}

enum E {
	E_C1(Test.f1);  // [1] 
	E(FI fi) {
	}
}

class C {	
	static int m(int x) {
		return x--;
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}