package simple18_out;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TestLambda1 {
	public static Supplier<Integer> add(int a, int b) {
		Supplier<Integer> supplier = () -> { return a + b; };
		Supplier<Integer> sum = supplier;
		return sum;
	}

	private static Supplier<Integer> m(int x, int y) {
		Supplier<Integer> supplier = () -> { return x + y; };
		return supplier;
	}
}
